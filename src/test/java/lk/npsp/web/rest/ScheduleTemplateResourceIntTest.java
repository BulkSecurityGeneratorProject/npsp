package lk.npsp.web.rest;

import lk.npsp.NpspApp;

import lk.npsp.domain.ScheduleTemplate;
import lk.npsp.repository.ScheduleTemplateRepository;
import lk.npsp.repository.search.ScheduleTemplateSearchRepository;
import lk.npsp.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static lk.npsp.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the ScheduleTemplateResource REST controller.
 *
 * @see ScheduleTemplateResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NpspApp.class)
public class ScheduleTemplateResourceIntTest {

    private static final Instant DEFAULT_START_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private ScheduleTemplateRepository scheduleTemplateRepository;
    @Mock
    private ScheduleTemplateRepository scheduleTemplateRepositoryMock;

    /**
     * This repository is mocked in the lk.npsp.repository.search test package.
     *
     * @see lk.npsp.repository.search.ScheduleTemplateSearchRepositoryMockConfiguration
     */
    @Autowired
    private ScheduleTemplateSearchRepository mockScheduleTemplateSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restScheduleTemplateMockMvc;

    private ScheduleTemplate scheduleTemplate;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ScheduleTemplateResource scheduleTemplateResource = new ScheduleTemplateResource(scheduleTemplateRepository, mockScheduleTemplateSearchRepository);
        this.restScheduleTemplateMockMvc = MockMvcBuilders.standaloneSetup(scheduleTemplateResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ScheduleTemplate createEntity(EntityManager em) {
        ScheduleTemplate scheduleTemplate = new ScheduleTemplate()
            .startTime(DEFAULT_START_TIME)
            .endTime(DEFAULT_END_TIME);
        return scheduleTemplate;
    }

    @Before
    public void initTest() {
        scheduleTemplate = createEntity(em);
    }

    @Test
    @Transactional
    public void createScheduleTemplate() throws Exception {
        int databaseSizeBeforeCreate = scheduleTemplateRepository.findAll().size();

        // Create the ScheduleTemplate
        restScheduleTemplateMockMvc.perform(post("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(scheduleTemplate)))
            .andExpect(status().isCreated());

        // Validate the ScheduleTemplate in the database
        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeCreate + 1);
        ScheduleTemplate testScheduleTemplate = scheduleTemplateList.get(scheduleTemplateList.size() - 1);
        assertThat(testScheduleTemplate.getStartTime()).isEqualTo(DEFAULT_START_TIME);
        assertThat(testScheduleTemplate.getEndTime()).isEqualTo(DEFAULT_END_TIME);

        // Validate the ScheduleTemplate in Elasticsearch
        verify(mockScheduleTemplateSearchRepository, times(1)).save(testScheduleTemplate);
    }

    @Test
    @Transactional
    public void createScheduleTemplateWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = scheduleTemplateRepository.findAll().size();

        // Create the ScheduleTemplate with an existing ID
        scheduleTemplate.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restScheduleTemplateMockMvc.perform(post("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(scheduleTemplate)))
            .andExpect(status().isBadRequest());

        // Validate the ScheduleTemplate in the database
        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeCreate);

        // Validate the ScheduleTemplate in Elasticsearch
        verify(mockScheduleTemplateSearchRepository, times(0)).save(scheduleTemplate);
    }

    @Test
    @Transactional
    public void checkStartTimeIsRequired() throws Exception {
        int databaseSizeBeforeTest = scheduleTemplateRepository.findAll().size();
        // set the field null
        scheduleTemplate.setStartTime(null);

        // Create the ScheduleTemplate, which fails.

        restScheduleTemplateMockMvc.perform(post("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(scheduleTemplate)))
            .andExpect(status().isBadRequest());

        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkEndTimeIsRequired() throws Exception {
        int databaseSizeBeforeTest = scheduleTemplateRepository.findAll().size();
        // set the field null
        scheduleTemplate.setEndTime(null);

        // Create the ScheduleTemplate, which fails.

        restScheduleTemplateMockMvc.perform(post("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(scheduleTemplate)))
            .andExpect(status().isBadRequest());

        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllScheduleTemplates() throws Exception {
        // Initialize the database
        scheduleTemplateRepository.saveAndFlush(scheduleTemplate);

        // Get all the scheduleTemplateList
        restScheduleTemplateMockMvc.perform(get("/api/schedule-templates?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(scheduleTemplate.getId().intValue())))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())));
    }
    
    public void getAllScheduleTemplatesWithEagerRelationshipsIsEnabled() throws Exception {
        ScheduleTemplateResource scheduleTemplateResource = new ScheduleTemplateResource(scheduleTemplateRepositoryMock, mockScheduleTemplateSearchRepository);
        when(scheduleTemplateRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        MockMvc restScheduleTemplateMockMvc = MockMvcBuilders.standaloneSetup(scheduleTemplateResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();

        restScheduleTemplateMockMvc.perform(get("/api/schedule-templates?eagerload=true"))
        .andExpect(status().isOk());

        verify(scheduleTemplateRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    public void getAllScheduleTemplatesWithEagerRelationshipsIsNotEnabled() throws Exception {
        ScheduleTemplateResource scheduleTemplateResource = new ScheduleTemplateResource(scheduleTemplateRepositoryMock, mockScheduleTemplateSearchRepository);
            when(scheduleTemplateRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));
            MockMvc restScheduleTemplateMockMvc = MockMvcBuilders.standaloneSetup(scheduleTemplateResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();

        restScheduleTemplateMockMvc.perform(get("/api/schedule-templates?eagerload=true"))
        .andExpect(status().isOk());

            verify(scheduleTemplateRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    @Transactional
    public void getScheduleTemplate() throws Exception {
        // Initialize the database
        scheduleTemplateRepository.saveAndFlush(scheduleTemplate);

        // Get the scheduleTemplate
        restScheduleTemplateMockMvc.perform(get("/api/schedule-templates/{id}", scheduleTemplate.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(scheduleTemplate.getId().intValue()))
            .andExpect(jsonPath("$.startTime").value(DEFAULT_START_TIME.toString()))
            .andExpect(jsonPath("$.endTime").value(DEFAULT_END_TIME.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingScheduleTemplate() throws Exception {
        // Get the scheduleTemplate
        restScheduleTemplateMockMvc.perform(get("/api/schedule-templates/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateScheduleTemplate() throws Exception {
        // Initialize the database
        scheduleTemplateRepository.saveAndFlush(scheduleTemplate);

        int databaseSizeBeforeUpdate = scheduleTemplateRepository.findAll().size();

        // Update the scheduleTemplate
        ScheduleTemplate updatedScheduleTemplate = scheduleTemplateRepository.findById(scheduleTemplate.getId()).get();
        // Disconnect from session so that the updates on updatedScheduleTemplate are not directly saved in db
        em.detach(updatedScheduleTemplate);
        updatedScheduleTemplate
            .startTime(UPDATED_START_TIME)
            .endTime(UPDATED_END_TIME);

        restScheduleTemplateMockMvc.perform(put("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedScheduleTemplate)))
            .andExpect(status().isOk());

        // Validate the ScheduleTemplate in the database
        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeUpdate);
        ScheduleTemplate testScheduleTemplate = scheduleTemplateList.get(scheduleTemplateList.size() - 1);
        assertThat(testScheduleTemplate.getStartTime()).isEqualTo(UPDATED_START_TIME);
        assertThat(testScheduleTemplate.getEndTime()).isEqualTo(UPDATED_END_TIME);

        // Validate the ScheduleTemplate in Elasticsearch
        verify(mockScheduleTemplateSearchRepository, times(1)).save(testScheduleTemplate);
    }

    @Test
    @Transactional
    public void updateNonExistingScheduleTemplate() throws Exception {
        int databaseSizeBeforeUpdate = scheduleTemplateRepository.findAll().size();

        // Create the ScheduleTemplate

        // If the entity doesn't have an ID, it will throw BadRequestAlertException 
        restScheduleTemplateMockMvc.perform(put("/api/schedule-templates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(scheduleTemplate)))
            .andExpect(status().isBadRequest());

        // Validate the ScheduleTemplate in the database
        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeUpdate);

        // Validate the ScheduleTemplate in Elasticsearch
        verify(mockScheduleTemplateSearchRepository, times(0)).save(scheduleTemplate);
    }

    @Test
    @Transactional
    public void deleteScheduleTemplate() throws Exception {
        // Initialize the database
        scheduleTemplateRepository.saveAndFlush(scheduleTemplate);

        int databaseSizeBeforeDelete = scheduleTemplateRepository.findAll().size();

        // Get the scheduleTemplate
        restScheduleTemplateMockMvc.perform(delete("/api/schedule-templates/{id}", scheduleTemplate.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<ScheduleTemplate> scheduleTemplateList = scheduleTemplateRepository.findAll();
        assertThat(scheduleTemplateList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the ScheduleTemplate in Elasticsearch
        verify(mockScheduleTemplateSearchRepository, times(1)).deleteById(scheduleTemplate.getId());
    }

    @Test
    @Transactional
    public void searchScheduleTemplate() throws Exception {
        // Initialize the database
        scheduleTemplateRepository.saveAndFlush(scheduleTemplate);
        when(mockScheduleTemplateSearchRepository.search(queryStringQuery("id:" + scheduleTemplate.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(scheduleTemplate), PageRequest.of(0, 1), 1));
        // Search the scheduleTemplate
        restScheduleTemplateMockMvc.perform(get("/api/_search/schedule-templates?query=id:" + scheduleTemplate.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(scheduleTemplate.getId().intValue())))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ScheduleTemplate.class);
        ScheduleTemplate scheduleTemplate1 = new ScheduleTemplate();
        scheduleTemplate1.setId(1L);
        ScheduleTemplate scheduleTemplate2 = new ScheduleTemplate();
        scheduleTemplate2.setId(scheduleTemplate1.getId());
        assertThat(scheduleTemplate1).isEqualTo(scheduleTemplate2);
        scheduleTemplate2.setId(2L);
        assertThat(scheduleTemplate1).isNotEqualTo(scheduleTemplate2);
        scheduleTemplate1.setId(null);
        assertThat(scheduleTemplate1).isNotEqualTo(scheduleTemplate2);
    }
}
