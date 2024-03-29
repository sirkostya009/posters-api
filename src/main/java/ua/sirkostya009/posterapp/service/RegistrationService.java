package ua.sirkostya009.posterapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.sirkostya009.posterapp.email.ConfirmationEmailSender;
import ua.sirkostya009.posterapp.email.EmailSender;
import ua.sirkostya009.posterapp.exception.EmailNotSentException;
import ua.sirkostya009.posterapp.exception.NotFoundException;
import ua.sirkostya009.posterapp.dto.RegistrationRequest;
import ua.sirkostya009.posterapp.dao.AppUser;
import ua.sirkostya009.posterapp.dao.ConfirmationToken;
import ua.sirkostya009.posterapp.exception.InvalidTokenException;
import ua.sirkostya009.posterapp.exception.OccupiedException;
import ua.sirkostya009.posterapp.repo.ConfirmationTokenRepo;
import ua.sirkostya009.posterapp.repo.UserRepo;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final ConfirmationTokenRepo tokenRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final EmailSender emailSender;

    /**
     * Checks if token exists,
     * checks if token hasn't been confirmed yet,
     * enables the user token points to,
     * sets the email to the user,
     * sets token as confirmed.
     * @param token token string to confirm
     * @throws InvalidTokenException if token is invalid
     * @throws NotFoundException if token is non-existent
     */
    @Transactional
    public void confirm(String token) {
        var confirmationToken = tokenRepo.findByToken(token)
                .orElseThrow(NotFoundException.supplier("token " + token + " not found"));

        if (confirmationToken.getConfirmedAt() != null)
            throw new InvalidTokenException("token " + token + " outdated");

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidTokenException("token " + token + " expired");

        confirmationToken.getAppUser().enable();
        confirmationToken.getAppUser().setEmail(confirmationToken.getEmail());

        confirmationToken.setConfirmedAt(LocalDateTime.now());
    }

    /**
     * Checks if provided username wasn't taken,
     * checks if provided email wasn't taken,
     * creates and saves user with encoded password to repo,
     * creates a token that points to newly created user,
     * sends an email to the provided email address
     * @param request a wrapper with username, email, and password fields
     * @return confirmation token string to confirm
     * @throws OccupiedException if username or emails have already been taken
     * @throws EmailNotSentException if failed to send email
     */
    public String register(RegistrationRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent())
            throw new OccupiedException("username already taken");

        if (userRepo.findByEmail(request.getEmail()).isPresent())
            throw new OccupiedException("email already taken");

        var user = new AppUser(request.getUsername(), encoder.encode(request.getPassword()));

        var token = new ConfirmationToken(
                UUID.randomUUID().toString(),
                user,
                request.getEmail(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15)
        );

        emailSender.send(request.getEmail(), "Confirm email", ConfirmationEmailSender.generateBody(token.getToken()));

        // save user and token only after email has been successfully sent
        userRepo.save(user);
        tokenRepo.save(token);

        return token.getToken();
    }

}
