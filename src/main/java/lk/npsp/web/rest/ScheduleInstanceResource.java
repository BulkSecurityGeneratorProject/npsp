package lk.npsp.web.rest;

import lk.npsp.domain.ScheduleInstance;
import lk.npsp.domain.ScheduleTemplate;
import lk.npsp.repository.ScheduleInstanceRepository;
import lk.npsp.repository.ScheduleTemplateRepository;
import lk.npsp.service.ScheduleInstanceManager;
import lk.npsp.web.rest.errors.BadRequestAlertException;
import lk.npsp.web.rest.util.HeaderUtil;
import lk.npsp.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ScheduleInstance.
 */
@RestController
@RequestMapping("/api")
public class ScheduleInstanceResource {

    private final Logger log = LoggerFactory.getLogger(ScheduleInstanceResource.class);

    private static final String ENTITY_NAME = "scheduleInstance";

    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleTemplateRepository scheduleTemplateRepository;
    private final ScheduleInstanceManager scheduleInstanceManager;

    public ScheduleInstanceResource(ScheduleInstanceRepository scheduleInstanceRepository,
                                    ScheduleTemplateRepository scheduleTemplateRepository,
                                    ScheduleInstanceManager scheduleInstanceManager) {
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleTemplateRepository = scheduleTemplateRepository;
        this.scheduleInstanceManager = scheduleInstanceManager;
    }

    /**
     * POST  /schedule-instances : Create a new scheduleInstance.
     *
     * @param scheduleInstance the scheduleInstance to create
     * @return the ResponseEntity with status 201 (Created) and with body the new scheduleInstance, or with status 400 (Bad Request) if the scheduleInstance has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/schedule-instances")
    public ResponseEntity<ScheduleInstance> createScheduleInstance(@RequestBody ScheduleInstance scheduleInstance) throws URISyntaxException {
        log.debug("REST request to save ScheduleInstance : {}", scheduleInstance);
        if (scheduleInstance.getId() != null) {
            throw new BadRequestAlertException("A new scheduleInstance cannot already have an ID", ENTITY_NAME, "idexists");
        }

        if (scheduleInstance.getScheduleTemplate() != null) {
            Optional<ScheduleTemplate> scheduleTemplateOptional = scheduleTemplateRepository.
                findById(scheduleInstance.getScheduleTemplate().getId());
            if (scheduleTemplateOptional.isPresent()) {
                scheduleInstance.setScheduleTemplate(scheduleTemplateOptional.get());
                scheduleInstance = scheduleInstanceManager.createFromTemplate
                    (scheduleInstance, scheduleTemplateOptional.get());
            }
        }

        ScheduleInstance result = scheduleInstanceRepository.save(scheduleInstance);
        return ResponseEntity.created(new URI("/api/schedule-instances/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(scheduleInstance);
    }

    /**
     * PUT  /schedule-instances : Updates an existing scheduleInstance.
     *
     * @param scheduleInstance the scheduleInstance to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated scheduleInstance,
     * or with status 400 (Bad Request) if the scheduleInstance is not valid,
     * or with status 500 (Internal Server Error) if the scheduleInstance couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/schedule-instances")
    public ResponseEntity<ScheduleInstance> updateScheduleInstance(@RequestBody ScheduleInstance scheduleInstance) throws URISyntaxException {
        log.debug("REST request to update ScheduleInstance : {}", scheduleInstance);
        if (scheduleInstance.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ScheduleInstance result = scheduleInstanceRepository.save(scheduleInstance);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, scheduleInstance.getId().toString()))
            .body(result);
    }

    /**
     * GET  /schedule-instances : get all the scheduleInstances.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of scheduleInstances in body
     */
    @GetMapping("/schedule-instances")
    public ResponseEntity<List<ScheduleInstance>> getAllScheduleInstances(Pageable pageable) {
        log.debug("REST request to get a page of ScheduleInstances");
        Page<ScheduleInstance> page = scheduleInstanceRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/schedule-instances");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /all-schedule-instances : get all the schedule-instances.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of schedule-instances in body
     */
    @GetMapping("/all-schedule-instances")
    public ResponseEntity<List<ScheduleInstance>> getAllScheduleInstances() {
        log.debug("REST request to get a list of ScheduleInstances");
        List<ScheduleInstance> list = scheduleInstanceRepository.findAll();
        return ResponseEntity.ok().body(list);
    }

    /**
     * GET  /schedule-operations : get all the scheduleInstances of the day.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of scheduleInstances in body
     */
    @GetMapping("/schedule-operations")
    public ResponseEntity<List<ScheduleInstance>> getScheduleOperations(Pageable pageable, @RequestParam("search") String search) {
        log.debug("REST request to get a page of Schedule Operations " + search);
        LocalDate currentDate = new java.sql.Date(new Date().getTime()).toLocalDate();
        Page<ScheduleInstance> page = scheduleInstanceRepository.findScheduleInstancesByDate
            (pageable, currentDate, search);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/schedule-instances");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /schedule-instances/:id : get the "id" scheduleInstance.
     *
     * @param id the id of the scheduleInstance to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the scheduleInstance, or with status 404 (Not Found)
     */
    @GetMapping("/schedule-instances/{id}")
    public ResponseEntity<ScheduleInstance> getScheduleInstance(@PathVariable Long id) {
        log.debug("REST request to get ScheduleInstance : {}", id);
        Optional<ScheduleInstance> scheduleInstance = scheduleInstanceRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(scheduleInstance);
    }

    /**
     * DELETE  /schedule-instances/:id : delete the "id" scheduleInstance.
     *
     * @param id the id of the scheduleInstance to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/schedule-instances/{id}")
    public ResponseEntity<Void> deleteScheduleInstance(@PathVariable Long id) {
        log.debug("REST request to delete ScheduleInstance : {}", id);
        scheduleInstanceRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
