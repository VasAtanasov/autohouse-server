package bg.autohouse.service.services;

import bg.autohouse.data.models.User;
import bg.autohouse.service.models.DealershipServiceModel;
import bg.autohouse.service.models.RegisterServiceModel;
import bg.autohouse.service.models.UserServiceModel;
import bg.autohouse.web.models.request.UserDetailsUpdateRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
  UserDetails loadUserById(String id);

  boolean existsByUsername(String username);

  boolean existsByDealershipName(String name);

  UserServiceModel register(RegisterServiceModel model);

  DealershipServiceModel registerDealer(String userId, DealershipServiceModel dealer);

  UserServiceModel updateUser(String userId, UserDetailsUpdateRequest user, User loggedUser);

  boolean verifyEmailToken(String token);

  boolean requestPasswordReset(String username);

  boolean resetPassword(String token, String password);
}
