package bg.autohouse.service.services.impl;

import bg.autohouse.data.models.User;
import bg.autohouse.data.models.UserCreateRequest;
import bg.autohouse.data.models.enums.Role;
import bg.autohouse.data.models.enums.UserLogType;
import bg.autohouse.data.repositories.UserRepository;
import bg.autohouse.data.repositories.UserRequestRepository;
import bg.autohouse.errors.ExceptionsMessages;
import bg.autohouse.errors.ResourceAlreadyExistsException;
import bg.autohouse.security.jwt.JwtToken;
import bg.autohouse.service.models.UserRegisterServiceModel;
import bg.autohouse.service.models.UserServiceModel;
import bg.autohouse.service.services.AsyncUserLogger;
import bg.autohouse.service.services.PasswordService;
import bg.autohouse.service.services.UserService;
import bg.autohouse.util.Assert;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.models.request.UserDetailsUpdateRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserRequestRepository userRequestRepository;
  private final PasswordService passwordService;
  private final ModelMapperWrapper modelMapper;
  private final PasswordEncoder encoder;
  private final AsyncUserLogger asyncUserService;

  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public UserDetails loadUserById(String id) {
    return userRepository
        .findById(id)
        .orElseThrow(
            () -> new UsernameNotFoundException(ExceptionsMessages.EXCEPTION_USER_NOT_FOUND_ID));
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public UserDetails loadUserByUsername(String username) {
    if (username == null) {
      throw new UsernameNotFoundException(ExceptionsMessages.INVALID_USER_LOGIN);
    }
    return userRepository
        .findByUsernameIgnoreCase(username)
        .orElseThrow(() -> new UsernameNotFoundException(ExceptionsMessages.NO_SUCH_USERNAME));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsernameIgnoreCase(username);
  }

  @Override
  public String generateUserRegistrationVerifier(UserRegisterServiceModel model) {
    Assert.notNull(model, "Model is required");

    if (existsByUsername(model.getUsername())) {
      throw new ResourceAlreadyExistsException(ExceptionsMessages.USER_ALREADY_EXISTS);
    }
    // TODO decide to save password in request or not
    if (!userRequestRepository.existsByUsernameIgnoreCase(model.getUsername())) {
      UserCreateRequest request = modelMapper.map(model, UserCreateRequest.class);
      log.info("Hashing password...");
      request.setPassword(encoder.encode(model.getPassword()));
      userRequestRepository.save(request);
    }

    JwtToken token = passwordService.generateRegistrationToken(model.getUsername());

    return token.getValue();
  }

  @Override
  @Transactional
  public UserServiceModel register(UserRegisterServiceModel model) {
    Assert.notNull(model, "Model object is required");
    Assert.notNull(model.getUsername(), "User email address is required");

    log.info("about to try to use email : {}", model.getUsername());

    String email = model.getUsername();
    long start = System.nanoTime();
    boolean userExists = Assert.has(email) && existsByUsername(email);
    long time = System.nanoTime() - start;
    log.info("User exists check took {} nano seconds", time);

    if (userExists) {
      throw new ResourceAlreadyExistsException(ExceptionsMessages.USER_ALREADY_EXISTS);
    }

    User user = modelMapper.map(model, User.class);
    // TODO How to decode password
    if (encoder != null) {
      // log.info("Hashing password...");
      // user.setPassword(encoder.encode(model.getPassword()));
    } else {
      log.warn("PasswordEncoder not set, skipping password encryption...");
    }

    log.info("Saving user...");
    user.setRoles(getInheritedRolesFromRole(Role.USER));
    userRepository.saveAndFlush(user);
    asyncUserService.recordUserLog(
        user.getId(), UserLogType.CREATED_IN_DB, "User registration created");
    return modelMapper.map(user, UserServiceModel.class);
  }

  // @Override
  // public DealershipServiceModel registerDealer(String userId, DealershipServiceModel dealer) {

  // if (existsByDealershipName(dealer.getName())) {
  //   throw new ResourceAlreadyExistsException(
  //       String.format(ExceptionsMessages.DEALERSHIP_ALREADY_EXISTS, dealer.getName()));
  // }
  // Address address =
  //     addressRepository
  //         .findById(dealer.getLocationId())
  //         .orElseThrow(() -> new NotFoundException(ExceptionsMessages.INVALID_LOCATION));
  // User user =
  //     userRepository
  //         .findById(userId)
  //         .orElseThrow(
  //             () ->
  //                 new
  // UsernameNotFoundException(ExceptionsMessages.EXCEPTION_USER_NOT_FOUND_ID));

  // Dealership dealership = modelMapper.map(dealer, Dealership.class);
  // dealership.setDealer(user);
  // dealershipRepository.save(dealership);
  // log.info("Saved dealership with name: {}", dealership.getName());
  // return modelMapper.map(dealership, DealershipServiceModel.class);

  //   return null;
  // }

  @Override
  public UserServiceModel updateUser(
      String userId, UserDetailsUpdateRequest user, User loggedUser) {

    User userEntity =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException(ExceptionsMessages.NO_RECORD_FOUND));

    if (!userEntity.getUsername().equals(loggedUser.getUsername())) {
      throw new IllegalStateException(ExceptionsMessages.INVALID_UPDATE_OPERATION);
    }

    // userEntity.setFirstName(user.getFirstName());
    // userEntity.setLastName(user.getLastName());

    userRepository.save(userEntity);

    return modelMapper.map(userEntity, UserServiceModel.class);
  }

  @Override
  public UserRegisterServiceModel loadUserCreateRequest(String username) {
    return userRequestRepository
        .findByUsername(username)
        .map(request -> modelMapper.map(request, UserRegisterServiceModel.class))
        .orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "No user registration request exists by username: " + username));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean userExist(String username) {
    return userRepository.existsByUsernameIgnoreCase(username);
  }

  private Set<Role> getInheritedRolesFromRole(Role role) {
    List<Role> allRoles = Arrays.stream(Role.values()).collect(Collectors.toList());
    int index = allRoles.indexOf(role);
    return new HashSet<>(allRoles.subList(index, allRoles.size()));
  }
}
