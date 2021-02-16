package winterschoolone;

import winterschoolone.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    ShopRepository shopRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayed_(@Payload Payed payed){

    	if(payed.isMe()){
            System.out.println("##### listener  : " + payed.toJson());
            
            Shop shop = new Shop();
            shop.setMenuId(payed.getMenuId());
            shop.setOrderId(payed.getOrderId());
            shop.setQty(payed.getQty());
            shop.setUserId(payed.getUserId());
            
            shopRepository.save(shop);
        }
    }

}
