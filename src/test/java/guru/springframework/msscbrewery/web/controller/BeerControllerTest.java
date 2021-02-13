package guru.springframework.msscbrewery.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.msscbrewery.services.BeerService;
import guru.springframework.msscbrewery.web.model.BeerDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;

@RunWith(SpringRunner.class)
@WebMvcTest(BeerController.class)
@AutoConfigureRestDocs(uriHost = "somesite.com", uriPort = 80)
public class BeerControllerTest {

    @MockBean
    BeerService beerService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    BeerDto validBeer;

    ConstraintDescriptions beerDtoConstraints = new ConstraintDescriptions(BeerDto.class);
    ConstraintsDescriptor beerDtoConstraintsDescriptor = new ConstraintsDescriptor(BeerDto.class);

    @Before
    public void setUp() {
        validBeer = BeerDto.builder().id(UUID.randomUUID())
                .beerName("Beer1")
                .beerStyle("PALE_ALE")
                .upc(123456789012L)
                .build();
    }

    @Test
    public void getBeer() throws Exception {
        given(beerService.getBeerById(any(UUID.class))).willReturn(validBeer);

        mockMvc.perform(get("/api/v1/beer/{beerId}", validBeer.getId().toString()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(validBeer.getId().toString())))
                .andExpect(jsonPath("$.beerName", is("Beer1")))
                .andDo(document(
                        "api/v1/beer-get",
                        pathParameters(
                                parameterWithName("beerId").description("The id of the beer to get")
                        )));
    }

    @Test
    public void handlePost() throws Exception {
        //given
        BeerDto beerDto = validBeer;
        beerDto.setId(null);
        BeerDto savedDto = BeerDto.builder().id(UUID.randomUUID()).beerName("New Beer").build();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        given(beerService.saveNewBeer(any())).willReturn(savedDto);

        mockMvc.perform(post("/api/v1/beer/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isCreated())
                .andDo(document("api/v1/beer-save",
                        requestFields(
                                fieldWithPath("id").ignored(),
                                fieldWithPath("beerName")
                                        .description("Beer name")
                                        .attributes(key("constraints").value(beerDtoConstraints.descriptionsForProperty("beerName"))),
                                fieldWithPath("beerStyle")
                                        .description("Beer style")
                                        .attributes(key("constraints").value(beerDtoConstraints.descriptionsForProperty("beerStyle"))),
                                fieldWithPath("upc")
                                        .description("UPC")
                                        .attributes(key("constraints").value(beerDtoConstraints.descriptionsForProperty("upc"))),
                                fieldWithPath("createdDate")
                                        .description("Created date")
                                        .attributes(key("constraints").value(beerDtoConstraints.descriptionsForProperty("createdDate"))),
                                fieldWithPath("lastUpdatedDate")
                                        .description("Last updated date")
                                        .attributes(key("constraints").value(beerDtoConstraints.descriptionsForProperty("lastUpdatedDate")))
                        )));

    }

    @Test
    public void handleUpdate() throws Exception {
        //given
        BeerDto beerDto = validBeer;
        beerDto.setId(null);
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        //when
        mockMvc.perform(put("/api/v1/beer/{beerId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isNoContent())
                .andDo(document("api/v1/beer-update",
                        pathParameters(
                                parameterWithName("beerId").description("The id of the beer to update")
                        ),
                        requestFields(
                                fieldWithPath("id").ignored(),
                                fieldWithPath("beerName")
                                        .description("Beer name")
                                        .attributes(beerDtoConstraintsDescriptor.constraints("beerName")),
                                fieldWithPath("beerStyle")
                                        .description("Beer style")
                                        .attributes(beerDtoConstraintsDescriptor.constraints("beerStyle")),
                                fieldWithPath("upc")
                                        .description("UPC")
                                        .attributes(beerDtoConstraintsDescriptor.constraints("upc")),
                                beerDtoConstraintsDescriptor.withPath("createdDate").description("Created date"),
                                beerDtoConstraintsDescriptor.withPath("lastUpdatedDate").description("Last updated date")
                        )));

        then(beerService).should().updateBeer(any(), any());
    }

    private static class ConstraintsDescriptor {
        private final ConstraintDescriptions beerDtoConstraints;

        public ConstraintsDescriptor(Class clazz) {
            beerDtoConstraints = new ConstraintDescriptions(clazz);
        }

        public Attributes.Attribute constraints(String path) {
            return key("constraints").value(beerDtoConstraints.descriptionsForProperty(path).stream().collect(Collectors.joining(".")));
        }

        public FieldDescriptor withPath(String path) {
            return fieldWithPath(path).attributes(constraints(path));
        }
    }
}