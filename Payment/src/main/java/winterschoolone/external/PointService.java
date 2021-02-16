package winterschoolone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Point", url="http://localhost:8085")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points")
    public void point(@RequestBody Point point);

}