package bg.autohouse.web.controllers;

import static bg.autohouse.config.WebConfiguration.APP_V1_MEDIA_TYPE_JSON;

import bg.autohouse.config.WebConfiguration;
import bg.autohouse.data.models.User;
import bg.autohouse.data.repositories.OfferRepository;
import bg.autohouse.security.authentication.LoggedUser;
import bg.autohouse.service.models.offer.OfferServiceModel;
import bg.autohouse.service.services.OfferService;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.enums.RestMessage;
import bg.autohouse.web.models.request.offer.OfferCreateRequest;
import bg.autohouse.web.models.response.offer.OfferDetailsResponseModel;
import bg.autohouse.web.models.response.offer.OfferDetailsResponseWrapper;
import bg.autohouse.web.models.response.offer.OfferResponseModel;
import bg.autohouse.web.util.RestUtil;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(WebConfiguration.OFFERS)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OfferController extends BaseController {

  private final OfferService offerService;
  private final ModelMapperWrapper modelMapper;
  private final OfferRepository offerRepository;

  @PostMapping(
      produces = {APP_V1_MEDIA_TYPE_JSON},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<?> createOffer(
      @Valid @ModelAttribute OfferCreateRequest createRequest, @LoggedUser User creator)
      throws IOException {
    OfferServiceModel offerServiceModel = offerService.createOffer(createRequest, creator.getId());
    return RestUtil.createSuccessResponse(
        offerServiceModel, RestMessage.OFFER_CREATED_SUCCESS, "/api/vehicles/offers");
  }

  @PostMapping(
      value = "/update/{offerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<?> updateOffer(
      @PathVariable UUID offerId,
      @Valid @ModelAttribute OfferCreateRequest createRequest,
      @LoggedUser User creator) {
    return ResponseEntity.ok(offerService.updateOffer(createRequest, offerId, creator.getId()));
  }

  @GetMapping(
      value = "/load-for-edit/{offerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getOfferForEdit(@PathVariable UUID offerId, @LoggedUser User creator) {
    return ResponseEntity.ok(offerService.loadOfferForEdit(creator.getId(), offerId));
  }

  @GetMapping(
      value = "/details/{offerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> viewOffer(
      @PathVariable UUID offerId,
      @RequestParam(value = "pr", required = false, defaultValue = "false") boolean pr,
      @LoggedUser User user) {
    OfferDetailsResponseModel offer;
    if (pr) {
      offer =
          modelMapper.map(
              offerService.loadOfferByIdPrivateView(offerId, user.getId()),
              OfferDetailsResponseModel.class);
    } else {
      offer =
          modelMapper.map(
              offerService.loadOfferByIdPublicView(offerId), OfferDetailsResponseModel.class);
    }
    List<String> imagesKeys = offerService.fetchOfferImages(offerId);
    return RestUtil.okResponse(
        OfferDetailsResponseWrapper.builder().offer(offer).images(imagesKeys).build());
  }

  @DeleteMapping(
      value = "/{offerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> deleteOffer(@PathVariable UUID offerId, @LoggedUser User user) {
    offerService.deleteOffer(user.getId(), offerId);
    return ResponseEntity.ok(offerId);
  }

  @GetMapping(
      value = "/top",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getLatestOffers() {
    List<OfferResponseModel> topOffers =
        modelMapper.mapAll(offerService.getLatestOffers(), OfferResponseModel.class);
    return RestUtil.okResponse(topOffers);
  }

  @GetMapping(
      value = "/statistics",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getOfferStatistics() {
    return ResponseEntity.ok(offerRepository.getStatistics());
  }

  @GetMapping(
      value = "/count/{accountId}/offers",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getUserOffersCount(@PathVariable UUID accountId) {
    return ResponseEntity.ok(offerRepository.countByAccountId(accountId));
  }
}
