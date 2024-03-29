package ua.sirkostya009.posterapp.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ua.sirkostya009.posterapp.dto.PosterInfo;
import ua.sirkostya009.posterapp.exception.NotFoundException;
import ua.sirkostya009.posterapp.service.PosterService;
import ua.sirkostya009.posterapp.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posters")
public class PosterApi {

    private final PosterService posterService;
    private final UserService userService;

    /**
     * Posts a poster
     * @param posterText text of poster to be published
     * @param token an object that holds current user's username
     * @return a published user
     */
    @PostMapping
    public PosterInfo post(@RequestBody String posterText,
                           JwtAuthenticationToken token) {
        var user = userService.findByUsername(token.getName());
        return PosterInfo.of(posterService.save(posterText, user), user, false);
    }

    /**
     * Returns a page of recently posted posters
     * @param page page number to be served
     * @param token an object that holds current user's username
     * @return page of posters
     */
    @GetMapping
    public Slice<PosterInfo> all(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                 JwtAuthenticationToken token) {
        var user = userService.findByUsername(token.getName());
        return posterService.recommendation(page, user)
                .map(poster -> PosterInfo.of(poster, user, true));
    }

    /**
     * Similar to all method, returns a page of posters but popular ones, sorted and filtered with a special algorithm
     * @param page page number to be served
     * @param token an object that holds current user's username
     * @return page of popular posters
     */
    @GetMapping("/popular")
    public Slice<PosterInfo> popular(@RequestParam(name = "page",  defaultValue = "0") Integer page,
                                     JwtAuthenticationToken token) {
        return posterService.popular(page)
                .map(poster -> PosterInfo.of(poster, userService.findByUsername(token.getName()), true));
    }

    /**
     * Returns a poster with a specified id
     * @param id provided id
     * @param token an object that holds current user's username
     * @return poster with a specified id
     * @exception NotFoundException if no poster was found
     */
    @GetMapping("/{id}")
    public PosterInfo id(@PathVariable Long id,
                         JwtAuthenticationToken token) {
        return PosterInfo.of(posterService.getPoster(id), userService.findByUsername(token.getName()), true);
    }

    /**
     * Likes post by the current user
     * @param id id of a poster to like
     * @param token an object that holds current user's username
     * @return true or false whether the user has liked or disliked the post
     * @exception NotFoundException if no poster was found
     */
    @GetMapping("/like/{id}")
    public boolean like(@PathVariable Long id,
                        JwtAuthenticationToken token) {
        return posterService.likePoster(id, userService.findByUsername(token.getName()));
    }

    /**
     * Edits poster's text
     * @param newText new text of a poster
     * @param id id of a poster to edit
     * @param token an object that holds current user's username
     * @return updated poster
     * @exception NotFoundException if no poster was found
     */
    @PostMapping("/edit/{id}")
    public PosterInfo edit(@RequestBody String newText,
                           @PathVariable Long id,
                           JwtAuthenticationToken token) {
        var user = userService.findByUsername(token.getName());
        return PosterInfo.of(
                posterService.editPoster(newText, id, user),
                user,
                false
        );
    }

    /**
     * Returns a page of most recent posters by user
     * @param username user to query posters of
     * @param page number of the page
     * @param token an object that holds current user's username
     * @return page of posters
     * @exception NotFoundException if no poster or username were found
     */
    @GetMapping("/by/{username}")
    public Slice<PosterInfo> userPosters(@PathVariable String username,
                                         @RequestParam(name = "value", defaultValue = "0") Integer page,
                                         JwtAuthenticationToken token) {
        return posterService.postersByUser(userService.findByUsername(username), page)
                .map(poster -> PosterInfo.of(poster, userService.findByUsername(token.getName()), true));
    }

}
