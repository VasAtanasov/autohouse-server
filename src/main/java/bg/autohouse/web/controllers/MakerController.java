package bg.autohouse.web.controllers;

import static bg.autohouse.config.WebConfiguration.APP_V1_MEDIA_TYPE_JSON;

import bg.autohouse.config.WebConfiguration;
import bg.autohouse.service.models.MakerModelServiceModel;
import bg.autohouse.service.models.MakerServiceModel;
import bg.autohouse.service.models.ModelServiceModel;
import bg.autohouse.service.models.ModelTrimsServicesModel;
import bg.autohouse.service.services.MakerService;
import bg.autohouse.util.Collect;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.enums.RestMessage;
import bg.autohouse.web.models.request.MakerCreateRequestModel;
import bg.autohouse.web.models.request.ModelCreateRequestModel;
import bg.autohouse.web.models.response.MakerResponseModel;
import bg.autohouse.web.models.response.MakerResponseWrapper;
import bg.autohouse.web.models.response.ModelTrimsResponseModel;
import bg.autohouse.web.util.RestUtil;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(WebConfiguration.URL_MAKERS)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MakerController extends BaseController {

  private final ModelMapperWrapper modelMapper;
  private final MakerService makerService;

  @GetMapping(produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getMakers() {
    List<MakerModelServiceModel> makerServiceModels = makerService.getAllMakerWithModels();
    Map<String, MakerResponseWrapper> makers =
        makerServiceModels.stream()
            .map(model -> modelMapper.map(model, MakerResponseWrapper.class))
            .collect(Collect.indexingBy(MakerResponseWrapper::getName));
    return RestUtil.okResponse(RestMessage.MAKERS_GET_SUCCESSFUL, makers);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(
      produces = {APP_V1_MEDIA_TYPE_JSON},
      consumes = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> createMaker(@Valid @RequestBody  MakerCreateRequestModel createRequest) {
    MakerServiceModel makerServiceModel = modelMapper.map(createRequest, MakerServiceModel.class);
    MakerServiceModel createdMaker = makerService.createMaker(makerServiceModel);
    MakerResponseModel data = modelMapper.map(createdMaker, MakerResponseModel.class);
    String locationURI = WebConfiguration.URL_API_BASE + WebConfiguration.URL_MAKERS;
    return RestUtil.createSuccessResponse(
        toMap("maker", data), RestMessage.MAKER_CREATED, locationURI);
  }

  @GetMapping(
      value = "/{makerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getMakerById(@Valid @PathVariable Long makerId) {
    MakerModelServiceModel model = makerService.getOne(makerId);
    MakerResponseWrapper maker = modelMapper.map(model, MakerResponseWrapper.class);
    return RestUtil.okResponse(RestMessage.MAKER_GET_SUCCESSFUL, toMap("maker", maker));
  }

  @GetMapping(
      value = "/{makerName}/{modelName}",
      produces = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> getByMakerNameModelName(
      @Valid @PathVariable String makerName, @Valid @PathVariable String modelName) {
    ModelTrimsServicesModel model = makerService.getModel(makerName, modelName);
    ModelTrimsResponseModel response = modelMapper.map(model, ModelTrimsResponseModel.class);
    return RestUtil.okResponse(RestMessage.MODEL_GET_SUCCESSFUL, toMap("model", response));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(
      value = "/{makerId}",
      produces = {APP_V1_MEDIA_TYPE_JSON},
      consumes = {APP_V1_MEDIA_TYPE_JSON})
  public ResponseEntity<?> createModel(
      @PathVariable Long makerId, @Valid @RequestBody ModelCreateRequestModel createRequest) {
    ModelServiceModel modelServiceModel = modelMapper.map(createRequest, ModelServiceModel.class);
    makerService.addModelToMaker(makerId, modelServiceModel);
    MakerResponseWrapper maker =
        modelMapper.map(makerService.getOne(makerId), MakerResponseWrapper.class);
    String locationURI =
        WebConfiguration.URL_API_BASE
            + WebConfiguration.URL_MAKERS
            + "/"
            + makerId
            + "/"
            + WebConfiguration.URL_MODELS;

    return RestUtil.createSuccessResponse(maker, RestMessage.MAKER_MODEL_CREATED, locationURI);
  }
}
