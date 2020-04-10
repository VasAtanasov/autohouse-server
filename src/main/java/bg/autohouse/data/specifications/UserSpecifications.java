package bg.autohouse.data.specifications;

import bg.autohouse.data.models.User;
import bg.autohouse.data.models.User_;
import java.util.Collection;
import java.util.Date;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

  public static Specification<User> isEnabled() {
    return (root, query, cb) -> cb.isTrue(root.get(User_.enabled));
  }

  public static Specification<User> createdAfter(Date start) {
    return (root, query, cb) -> cb.greaterThan(root.get(User_.createdAt), start);
  }

  public static Specification<User> createdBefore(Date end) {
    return (root, query, cb) -> cb.lessThan(root.get(User_.createdAt), end);
  }

  public static Specification<User> idIn(Collection<String> ids) {
    return (root, query, cb) -> root.get(User_.id).in(ids);
  }
}
