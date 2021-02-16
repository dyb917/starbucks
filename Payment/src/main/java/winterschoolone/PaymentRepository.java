package winterschoolone;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PaymentRepository extends PagingAndSortingRepository<Payment, Long>{

	List<Payment> findByOrderId(Long orderId);
}