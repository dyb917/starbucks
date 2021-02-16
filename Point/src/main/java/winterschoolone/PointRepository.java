package winterschoolone;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PointRepository extends PagingAndSortingRepository<Point, Long>{

	List<Point> findByOrderId(Long orderId);
}