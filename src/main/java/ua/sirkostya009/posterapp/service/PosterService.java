package ua.sirkostya009.posterapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.sirkostya009.posterapp.dao.AppUser;
import ua.sirkostya009.posterapp.dao.Hashtag;
import ua.sirkostya009.posterapp.dao.Poster;
import ua.sirkostya009.posterapp.exception.NotFoundException;
import ua.sirkostya009.posterapp.repo.HashtagRepo;
import ua.sirkostya009.posterapp.repo.PosterRepo;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PosterService {

    private final PosterRepo posterRepo;
    private final HashtagRepo hashtagRepo;

    private final static int POSTS_PER_PAGE = 10;
    private final static char[] CHARACTERS = ",!@#$%^&*()-+=|\\/?.№\";%:*~`'".toCharArray();

    public Poster getPoster(Long id) {
        return posterRepo.findById(id)
                .orElseThrow(NotFoundException.supplier("poster with id " + id + " could not be found"));
    }

    @Transactional
    public Poster save(String posterText, AppUser user) {
        return posterRepo.save(new Poster(posterText, user, parseHashtags(posterText)));
    }

    /**
     * Parses a given text for hashtags (words that begin with #),
     * finds already existing ones or saves new ones to repo,
     * collects everything to a set and increments mentions of each hashtag
     * @param posterText text to be parsed
     * @return set of hashtags
     */
    private Set<Hashtag> parseHashtags(String posterText) {
        var result = Stream.of(posterText.split(" "))
                .map(this::parseTag)
                .filter(Objects::nonNull)
                .map(tag -> hashtagRepo.findByTagIgnoreCase(tag)
                                .orElseGet(() -> hashtagRepo.save(new Hashtag(tag))))
                .collect(Collectors.toSet());

        result.forEach(Hashtag::incrementMentions); // for some reason I cant do this directly inside map method

        return result;
    }

    private String parseTag(String suspect) {
        if (!suspect.startsWith("#")) return null;

        var split = suspect.split("#");

        if (split.length == 0) return null;

        if (!split[0].equals("") || split.length == 1) return null;

        var tag = split[1];
        var maxIndex = 0;
        boolean doesntEndWithALetter = false;

        for (; maxIndex < tag.length(); ++maxIndex)
            for (var i : CHARACTERS)
                if (i == tag.charAt(maxIndex)) {
                    doesntEndWithALetter = true;
                    break;
                }

        if (0 == maxIndex) return null;

        return tag.substring(0, doesntEndWithALetter ? maxIndex - 1 : maxIndex);
    }

    /**
     * Currently, the recommendation-serving algorithm is so stupid, that I don't even want to document it
     * @param page page number
     * @param user current user
     * @return page of recommended posters
     */
    public Slice<Poster> recommendation(int page, AppUser user) {
        var postersByUser = posterRepo.findAllByAuthor(user, pageRequest(page));

        if (postersByUser.getContent().isEmpty()) return popular(page);

        var recommended = postersByUser.stream()
                .map(Poster::getHashtags)
                .flatMap(Collection::stream)
                .flatMap(hashtag -> posterRepo.findMostPopularWithTag(hashtag, pageRequest(page)).stream())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        return new SliceImpl<>(recommended, pageRequest(page), true);
    }

    @Transactional
    public boolean likePoster(Long id, AppUser user) {
        var poster = getPoster(id);

        if (poster.getLikes().contains(user))
            poster.getLikes().remove(user);
        else poster.getLikes().add(user);

        return poster.getLikes().contains(user);
    }

    @Transactional
    public Poster editPoster(String newText, Long id, AppUser user) {
        var poster = getPoster(id);

        if (!poster.getAuthor().getId().equals(user.getId()))
            throw new IllegalStateException("poster ownership unfulfilled");

        poster.getHashtags().forEach(Hashtag::decrementMentions);

        poster.setText(newText);
        poster.setLastEditedAt(LocalDateTime.now());
        poster.setHashtags(parseHashtags(newText));

        return poster;
    }

    public Slice<Poster> postersByUser(AppUser user, int pageNumber) {
        return posterRepo.findAllByAuthor(user, pageRequest(pageNumber));
    }

    /**
     * Returns posters that include most mentioned hashtags
     * Could be improved
     * @param page page number
     * @return a page of popular posters
     */
    public Slice<Poster> popular(int page) {
        var tags = hashtagRepo.findByMostMentions(pageRequest(page));
        var posters = new HashSet<Poster>(tags.getNumberOfElements());

        tags.getContent().forEach(hashtag ->
                posters.addAll(posterRepo.findMostPopularWithTag(hashtag, pageRequest(page)).getContent())
        );

        return new SliceImpl<>(posters.stream().toList(), pageRequest(page), true);
    }

    private PageRequest pageRequest(Integer page) {
        return PageRequest.of(page, POSTS_PER_PAGE);
    }

}
