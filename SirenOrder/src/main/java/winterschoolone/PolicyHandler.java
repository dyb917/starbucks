package winterschoolone;

import winterschoolone.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    
    @Autowired
	SirenOrderRepository sirenOrderRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAssigned_(@Payload Assigned assigned){

        if(assigned.isMe()){
        	Optional<SirenOrder> optional = sirenOrderRepository.findById(assigned.getOrderId());
        	if(optional != null && optional.isPresent())
        	{
        		SirenOrder sirenOrder = optional.get();
        		
        		sirenOrder.setStatus("Assigned");
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                // view 레파지 토리에 save
            	sirenOrderRepository.save(sirenOrder);
        	}
            
            System.out.println("##### listener  : " + assigned.toJson());
        }
    }

}
