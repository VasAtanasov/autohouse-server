package bg.autohouse.security.jwt;

import org.springframework.data.jpa.domain.Specification;

public class JwtTokenSpecifications {

  public static Specification<JwtToken> forUser(final String username) {
    return (root, query, cb) -> cb.equal(root.get(JwtToken_.username), username);
  }

  public static Specification<JwtToken> withTokenUid(final String tokenUid) {
    return (root, query, cb) -> cb.equal(root.get(JwtToken_.tokenUid), tokenUid);
  }

  public static Specification<JwtToken> withValue(final String value) {
    return (root, query, cb) -> cb.equal(root.get(JwtToken_.value), value);
  }

  public static Specification<JwtToken> withType(final JwtTokenType value) {
    return (root, query, cb) -> cb.equal(root.get(JwtToken_.type), value);
  }
}
