package bg.autohouse.web.controllers;

import static bg.autohouse.web.controllers.ResponseBodyMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.autohouse.MvcPerformer;
import bg.autohouse.common.Constants;
import bg.autohouse.data.repositories.MakerRepository;
import bg.autohouse.errors.ExceptionsMessages;
import bg.autohouse.errors.MakerNotFoundException;
import bg.autohouse.service.models.MakerModelServiceModel;
import bg.autohouse.service.models.MakerServiceModel;
import bg.autohouse.service.services.InitialStateService;
import bg.autohouse.service.services.MakerService;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.validation.ValidationMessages;
import bg.autohouse.web.models.request.MakerCreateRequestModel;
import bg.autohouse.web.models.request.ModelCreateRequestModel;
import bg.autohouse.web.models.response.MakerResponseModel;
import bg.autohouse.web.models.response.MakerResponseWrapper;
import bg.autohouse.web.models.response.ModelResponseModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MakerController.class)
public class MakerControllerTest extends MvcPerformer {
  static final String MAKER_NAME = "Audi";
  static final String API_BASE = "/api/vehicles";

  @Autowired private MockMvc mockMvc;

  @MockBean private MakerRepository makerRepository;
  @MockBean private MakerService makerService;
  @MockBean private InitialStateService initialStateService;
  @MockBean private ModelMapperWrapper modelMapper;

  @Override
  public MockMvc getMockMvc() {
    return mockMvc;
  }

  @Test
  void whenGetMakers_thenReturns200() throws Exception {
    List<MakerServiceModel> serviceModels = new ArrayList<>();
    List<MakerResponseModel> responseModels = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      MakerServiceModel serviceModel =
          MakerServiceModel.builder().id(Long.valueOf(i)).name("Maker" + i).build();
      serviceModels.add(serviceModel);

      MakerResponseModel responseModel =
          MakerResponseModel.builder().id(Long.valueOf(i)).name("Maker" + i).build();
      responseModels.add(responseModel);

      when(modelMapper.map(serviceModel, MakerResponseModel.class)).thenReturn(responseModel);
    }

    when(makerService.getAllMakers()).thenReturn(serviceModels);

    performGet(API_BASE + "/makers")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
        .andExpect(jsonPath("$.data.makers", hasSize(responseModels.size())));
  }

  @Test
  void whenGetMaker_withValidId_thenReturns200() throws Exception {

    MakerModelServiceModel model = MakerModelServiceModel.builder().id(1L).name(MAKER_NAME).build();
    MakerResponseWrapper response = MakerResponseWrapper.builder().id(1L).name(MAKER_NAME).build();

    when(makerService.getOne(1L)).thenReturn(model);
    when(modelMapper.map(model, MakerResponseWrapper.class)).thenReturn(response);

    performGet(API_BASE + "/makers/" + model.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
        .andExpect(jsonPath("$.data.maker.name", is(MAKER_NAME)))
        .andExpect(jsonPath("$.data.maker.id", is(1)));
  }

  @Test
  void whenInvalidId_thenReturns404() throws Exception {

    when(makerService.getOne(anyLong())).thenThrow(new MakerNotFoundException());

    performGet(API_BASE + "/makers/" + 12L)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())))
        .andExpect(jsonPath("$.message", is(ExceptionsMessages.EXCEPTION_MAKER_NOT_FOUND)));
  }

  @Test
  void whenInvalidMediaType_thenReturns406() throws Exception {

    mockMvc
        .perform(get(API_BASE + "/makers/").accept(MediaType.APPLICATION_JSON))
        // .andDo(print())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.status", is(HttpStatus.NOT_ACCEPTABLE.value())))
        .andExpect(status().isNotAcceptable());
  }

  @Test
  public void whenCreateModel_withValidBody_shouldReturn201() throws Exception {

    ModelResponseModel modelServiceModel = ModelResponseModel.builder().id(10L).name("A4").build();

    List<Object> responseModels = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      ModelResponseModel responseModel =
          ModelResponseModel.builder().id(Long.valueOf(i)).name("Model" + i).build();
      responseModels.add(responseModel);
    }
    responseModels.add(modelServiceModel);

    MakerModelServiceModel makerServiceModel =
        MakerModelServiceModel.builder().id(1L).name(MAKER_NAME).build();
    when(makerService.getOne(anyLong())).thenReturn(makerServiceModel);

    MakerResponseWrapper response = MakerResponseWrapper.builder().id(1L).name(MAKER_NAME).build();
    when(modelMapper.map(any(MakerModelServiceModel.class), any())).thenReturn(response);
    when(modelMapper.mapAll(anyList(), any())).thenReturn(responseModels);

    String expectedMessage = String.format(Constants.MODEL_CREATE_SUCCESS, "A4", MAKER_NAME);

    ModelCreateRequestModel createRequestModel = ModelCreateRequestModel.of("A4", 1L);
    performPost(API_BASE + "/makers/1", createRequestModel)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.message", is(expectedMessage)))
        .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())));
  }

  @Test
  public void whenCreateModel_withEmptyName_shouldReturn400() throws Exception {

    ModelCreateRequestModel createRequestModel = ModelCreateRequestModel.of("", 1L);

    performPost(API_BASE + "/makers/1", createRequestModel)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is(HttpStatus.BAD_REQUEST.getReasonPhrase())))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.errors", hasSize(2)));
  }

  @Test
  public void whenCreateModel_withNullName_shouldReturn400() throws Exception {

    ModelCreateRequestModel createRequestModel = ModelCreateRequestModel.of(null, 1L);

    performPost(API_BASE + "/makers/1", createRequestModel)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is(HttpStatus.BAD_REQUEST.getReasonPhrase())))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(responseBody().containsError("name", ValidationMessages.MODEL_NAME_BLANK));
  }

  @Test
  public void whenCreateModel_withNullId_shouldReturn400() throws Exception {

    ModelCreateRequestModel createRequestModel = ModelCreateRequestModel.of("A4", null);

    performPost(API_BASE + "/makers/1", createRequestModel)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is(HttpStatus.BAD_REQUEST.getReasonPhrase())))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(responseBody().containsError("makerId", ValidationMessages.MAKER_ID_NULL));
  }

  @Test
  public void whenCreateModel_witInvalidTypeId_shouldReturn400() throws Exception {

    String createModelJson = "{\"name\":\"A4\",\"makerId\":\"invalid_id\"}";

    performPost(API_BASE + "/makers/1", createModelJson)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is(ExceptionsMessages.INVALID_DATA_TYPE)))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));
  }

  @Test
  public void whenCreateMaker_withValidBody_shouldReturn201() throws Exception {
    MakerServiceModel makerServiceModel = MakerServiceModel.of(null, MAKER_NAME);
    when(modelMapper.map(any(MakerCreateRequestModel.class), any())).thenReturn(makerServiceModel);
    when(makerService.createMaker(any(MakerServiceModel.class))).thenReturn(makerServiceModel);

    MakerResponseModel response = MakerResponseModel.builder().id(1L).name(MAKER_NAME).build();
    when(modelMapper.map(any(MakerServiceModel.class), any())).thenReturn(response);

    String expectedMessage = String.format(Constants.MAKER_CREATE_SUCCESS, MAKER_NAME);
    MakerCreateRequestModel createRequestModel = MakerCreateRequestModel.of(MAKER_NAME);
    performPost(API_BASE + "/makers", createRequestModel)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.message", is(expectedMessage)))
        .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())))
        .andExpect(jsonPath("$.data.maker.name", is(MAKER_NAME)));
  }
}
