package voting;

/**
 * Created by Sanjay Iyer on 2/25/2015.
 */
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.validation.Valid;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.ApplicationContextException;
import voting.ModPollStorage;


@RestController


public class VotingController {
    private final AtomicLong midIncrement = new AtomicLong();
    @RequestMapping(value="api/v1/moderators", method=RequestMethod.POST, produces="application/json")
    //@ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> createModerator(@Validated(Moderator.ModeratorValidator.class) @RequestBody Moderator moderator, BindingResult result ) {
        if(result.hasErrors())
        {
            return new ResponseEntity<String>(callError(result),HttpStatus.BAD_REQUEST);
        }
        moderator.setId((int)(midIncrement.getAndIncrement()));
        String createdAt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(
                Calendar.getInstance().getTime());
        moderator.setCreated_at(createdAt);
        ModPollStorage.moderators.put(moderator.getId(), moderator);
        return new ResponseEntity<String>(moderator.toString(),HttpStatus.CREATED);
    }

    @RequestMapping(value="api/v1/moderators/{moderator_id}", method=RequestMethod.GET)
    public ResponseEntity<Moderator> viewModerator(@PathVariable("moderator_id") Integer modId){
        if(ModPollStorage.moderators.containsKey(modId)) {
            return new ResponseEntity<Moderator>(ModPollStorage.moderators.get(modId),HttpStatus.OK);
        }
        else
            return new ResponseEntity<Moderator>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="api/v1/moderators/{moderator_id}", method=RequestMethod.PUT, produces="application/json")
    public ResponseEntity<String> updateModerator(@Validated(Moderator.EmailValidator.class) @PathVariable("moderator_id") Integer modId,@RequestBody Moderator moderator, BindingResult result){
        if(result.hasErrors())
        {
            return new ResponseEntity<String>(callError(result),HttpStatus.BAD_REQUEST);
        }
        if(ModPollStorage.moderators.containsKey(modId)) {
            Moderator moderatorRemoved = ModPollStorage.moderators.remove(modId);
            moderator.setCreated_at(moderatorRemoved.getCreated_at());
            moderator.setId(modId);
            moderator.setName(moderatorRemoved.getName());
            ModPollStorage.moderators.put(modId, moderator);
            return new ResponseEntity<String>(moderator.toString(),HttpStatus.OK);
        }
        else
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    private String callError(BindingResult result) {
        StringBuilder errorMsg = new StringBuilder();
        for (ObjectError err: result.getAllErrors()){
            errorMsg.append(err.getDefaultMessage());
        }
        return errorMsg.toString();

    }
}