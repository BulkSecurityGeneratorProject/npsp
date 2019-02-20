package lk.npsp.web.rest;

import com.codahale.metrics.annotation.Timed;
import lk.npsp.domain.Weekday;
import lk.npsp.repository.WeekdayRepository;
import lk.npsp.repository.search.WeekdaySearchRepository;
import lk.npsp.web.rest.errors.BadRequestAlertException;
import lk.npsp.web.rest.util.HeaderUtil;
import lk.npsp.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Weekday.
 */
@RestController
@RequestMapping("/api")
public class WeekdayResource {

    private final Logger log = LoggerFactory.getLogger(WeekdayResource.class);

    private static final String ENTITY_NAME = "weekday";

    private final WeekdayRepository weekdayRepository;

    private final WeekdaySearchRepository weekdaySearchRepository;

    public WeekdayResource(WeekdayRepository weekdayRepository, WeekdaySearchRepository weekdaySearchRepository) {
        this.weekdayRepository = weekdayRepository;
        this.weekdaySearchRepository = weekdaySearchRepository;
    }

    /**
     * POST  /weekdays : Create a new weekday.
     *
     * @param weekday the weekday to create
     * @return the ResponseEntity with status 201 (Created) and with body the new weekday, or with status 400 (Bad Request) if the weekday has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/weekdays")
    @Timed
    public ResponseEntity<Weekday> createWeekday(@Valid @RequestBody Weekday weekday) throws URISyntaxException {
        log.debug("REST request to save Weekday : {}", weekday);
        if (weekday.getId() != null) {
            throw new BadRequestAlertException("A new weekday cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Weekday result = weekdayRepository.save(weekday);
        weekdaySearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/weekdays/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /weekdays : Updates an existing weekday.
     *
     * @param weekday the weekday to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated weekday,
     * or with status 400 (Bad Request) if the weekday is not valid,
     * or with status 500 (Internal Server Error) if the weekday couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/weekdays")
    @Timed
    public ResponseEntity<Weekday> updateWeekday(@Valid @RequestBody Weekday weekday) throws URISyntaxException {
        log.debug("REST request to update Weekday : {}", weekday);
        if (weekday.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Weekday result = weekdayRepository.save(weekday);
        weekdaySearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, weekday.getId().toString()))
            .body(result);
    }

    /**
     * GET  /weekdays : get all the weekdays.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of weekdays in body
     */
    @GetMapping("/weekdays")
    @Timed
    public ResponseEntity<List<Weekday>> getAllWeekdays(Pageable pageable) {
        log.debug("REST request to get a page of Weekdays");
        Page<Weekday> page = weekdayRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/weekdays");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /weekdays/:id : get the "id" weekday.
     *
     * @param id the id of the weekday to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the weekday, or with status 404 (Not Found)
     */
    @GetMapping("/weekdays/{id}")
    @Timed
    public ResponseEntity<Weekday> getWeekday(@PathVariable Long id) {
        log.debug("REST request to get Weekday : {}", id);
        Optional<Weekday> weekday = weekdayRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(weekday);
    }

    /**
     * DELETE  /weekdays/:id : delete the "id" weekday.
     *
     * @param id the id of the weekday to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/weekdays/{id}")
    @Timed
    public ResponseEntity<Void> deleteWeekday(@PathVariable Long id) {
        log.debug("REST request to delete Weekday : {}", id);

        weekdayRepository.deleteById(id);
        weekdaySearchRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/weekdays?query=:query : search for the weekday corresponding
     * to the query.
     *
     * @param query the query of the weekday search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping("/_search/weekdays")
    @Timed
    public ResponseEntity<List<Weekday>> searchWeekdays(@RequestParam String query, Pageable pageable) {
        log.debug("REST request to search for a page of Weekdays for query {}", query);
        Page<Weekday> page = weekdaySearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/weekdays");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

}
