package voting;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import voting.ModPollStorage;
import voting.Moderator;
/**
 * Created by Sanjay Iyer on 2/26/2015.
 */
@RestController
public class PollingRestController {
    private final AtomicLong pidIncrement = new AtomicLong();
    private boolean checkModeratorToPoll(Integer mod_id, String p_id){
        return ModPollStorage.modPolls.get(mod_id).contains(p_id);
    }
    @JsonView(Poll.ViewPoll.class)
    @RequestMapping(value="api/v1/moderators/{moderator_id}/polls", method=RequestMethod.POST, produces="application/json")
    public ResponseEntity<String> createPoll(@Valid @RequestBody Poll poll, @PathVariable("moderator_id") int modId) {
        if(ModPollStorage.moderators.containsKey(modId)) {
            poll.setPoll_id(Long.toString(pidIncrement.getAndIncrement(), 36).toUpperCase());
            Integer[] results = new Integer[poll.getChoice().length];
            for (int i = 0; i < results.length; i++) {

                results[i] = 0;
            }
            poll.setResults(results);
            ModPollStorage.polls.put(poll.getPoll_id(), poll);

            if (!ModPollStorage.modPolls.containsKey(modId)) {
                ArrayList<String> pollList = new ArrayList<String>();
                pollList.add(poll.getPoll_id());
                ModPollStorage.modPolls.put(modId, pollList);
            }
            else
                ModPollStorage.modPolls.get(modId).add(poll.getPoll_id());
        }
        else{
            return new ResponseEntity<String>(HttpStatus.FAILED_DEPENDENCY);
        }
        return new ResponseEntity<String>(poll.toString(),HttpStatus.CREATED);
    }

    @JsonView(Poll.ViewPoll.class)
    @RequestMapping(value="api/v1/polls/{poll_id}", method=RequestMethod.GET)
    public ResponseEntity<Poll> viewPollWithoutResult(@PathVariable("poll_id") String pollId){
        if(ModPollStorage.polls.containsKey(pollId))
        {
                return new ResponseEntity<Poll>(ModPollStorage.polls.get(pollId),HttpStatus.OK);
        }
        else
            return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(value="api/v1/moderators/{moderator_id}/polls/{poll_id}", method=RequestMethod.GET)
    public ResponseEntity<Poll> viewPollWithResult(@PathVariable("poll_id") String pollId, @PathVariable("moderator_id") int moderatorId){
       if(ModPollStorage.polls.containsKey(pollId) && ModPollStorage.moderators.containsKey(moderatorId))
       {
           if(checkModeratorToPoll(moderatorId,pollId))
               return new ResponseEntity<Poll>(ModPollStorage.polls.get(pollId),HttpStatus.OK);
           else
               return new ResponseEntity<Poll>(HttpStatus.FAILED_DEPENDENCY);
       }
       else
           return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(value="api/v1/moderators/{moderator_id}/polls", method=RequestMethod.GET)
    public ResponseEntity<ArrayList<Poll>> listAllPolls(@PathVariable("moderator_id") Integer moderatorId)
    {
        ArrayList<Poll> pollList = new ArrayList<Poll>();
        if(ModPollStorage.moderators.containsKey(moderatorId)) {
            ArrayList<String> pollIdList = ModPollStorage.modPolls.get(moderatorId);
            for(String pollId : pollIdList)
                pollList.add(ModPollStorage.polls.get(pollId));
            return new ResponseEntity<ArrayList<Poll>>(pollList,HttpStatus.OK);
        }
        else
            return new ResponseEntity<ArrayList<Poll>>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="api/v1/moderators/{moderator_id}/polls/{poll_id}", method=RequestMethod.DELETE)
    public ResponseEntity<Poll> deletePoll(@PathVariable("moderator_id") Integer moderatorId, @PathVariable("poll_id") String pollId )
    {
        if(ModPollStorage.moderators.containsKey(moderatorId) && ModPollStorage.polls.containsKey(pollId)){
            if(checkModeratorToPoll(moderatorId,pollId)) {
                ModPollStorage.polls.remove(pollId);
                ModPollStorage.modPolls.get(moderatorId).remove(pollId);
                if(ModPollStorage.modPolls.get(moderatorId).isEmpty()){
                    ModPollStorage.modPolls.remove(moderatorId);
                }
            }
            else
                return new ResponseEntity<Poll>(HttpStatus.FAILED_DEPENDENCY);
        }
        else
            return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<Poll>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value="api/v1/polls/{poll_id}", method=RequestMethod.PUT, produces="application/json")
    public ResponseEntity<String> Vote(@PathVariable("poll_id") String pollId, @RequestParam("choice") int choice){
        if(ModPollStorage.polls.containsKey(pollId)){
            Integer[] resultsUpdated=ModPollStorage.polls.get(pollId).getResults();
            if(choice<resultsUpdated.length) {
                resultsUpdated[choice]++;
                ModPollStorage.polls.get(pollId).setResults(resultsUpdated);
                return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
            }
            else {
                return new ResponseEntity<String>("Choice index is invalid!!",HttpStatus.BAD_REQUEST);
            }
        }
        else
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
}