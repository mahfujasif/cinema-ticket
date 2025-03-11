package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;


public class TicketServiceImpl implements TicketService {

    private static final Logger logger = Logger.getLogger(TicketServiceImpl.class.getName());

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;


    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Validate request parameters
     *
     * @param accountId The Account ID of the request user.
     * @param ticketTypeRequests List of ticket purchase requests.
     */
    private void validateRequestParameters(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        if(accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Account ID must be a positive number, Received: " + accountId);
        }
        if(ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new IllegalArgumentException("At least one ticket type must be provided.");
        }
    }

    /**
     * Counts the total number of ticket for each types
     *
     * @param ticketTypeRequests The TicketTypeRequest
     * @return A map of total ticket number for each types
     */
    private Map<TicketTypeRequest.Type, Integer> countTickets(TicketTypeRequest... ticketTypeRequests) {
        Map<TicketTypeRequest.Type, Integer> ticketCount = new EnumMap<>(TicketTypeRequest.Type.class);

        for(TicketTypeRequest ticketTypeRequest: ticketTypeRequests) {
            if(ticketTypeRequest != null) {
                TicketTypeRequest.Type type = ticketTypeRequest.getTicketType();
                int count = ticketCount.getOrDefault(type, 0);
                count += ticketTypeRequest.getNoOfTickets();
                ticketCount.put(type, count);
            }
        }
        return ticketCount;
    }

    /**
     * Validate constraints for ticket purchase
     *
     * @param ticketCount The map of the total number of individual ticket types.
     * @throws InvalidPurchaseException when fails any constraint
     */
    private void validateTicketCount(Map<TicketTypeRequest.Type, Integer> ticketCount) throws InvalidPurchaseException {
        int adultTicketCount = ticketCount.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
        int childTicketCount =  ticketCount.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
        int infantTicketCount =  ticketCount.getOrDefault(TicketTypeRequest.Type.INFANT, 0);

        // Maximum 25 tickets can be purchased at a time
        if((adultTicketCount + childTicketCount + infantTicketCount) > 25 ) {
            throw new InvalidPurchaseException("Number of total tickets must not be more than 25.");
        }

        // Child and infant ticket can not be purchased without adult ticket
        if(adultTicketCount == 0) {
            throw new InvalidPurchaseException("At least one adult ticket must be purchased");
        }

        // Number of infant ticket must be less than or equal to adult ticket
        if(adultTicketCount < infantTicketCount) {
            throw new InvalidPurchaseException("Number of infant tickets must not be more than number of adult tickets.");
        }
    }

    /**
     * Calculate the total price for all tickets.
     *
     * @param ticketCount The map of the total number of individual ticket types.
     * @return Total price.
     */
    private int calculateTotalPrice(Map<TicketTypeRequest.Type, Integer> ticketCount) {
        int totalPrice = 0;
        for(Map.Entry<TicketTypeRequest.Type, Integer> entry: ticketCount.entrySet()) {
            totalPrice += entry.getValue() * entry.getKey().getPrice();
        }
        return totalPrice;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        logger.info("Request received to purchase ticket for Account: " + accountId);
        this.validateRequestParameters(accountId, ticketTypeRequests);
        Map<TicketTypeRequest.Type, Integer> ticketCount = this.countTickets(ticketTypeRequests);
        this.validateTicketCount(ticketCount);
        ticketPaymentService.makePayment(accountId, this.calculateTotalPrice(ticketCount));

        //Infants do not require seats
        int totalSeats = ticketCount.entrySet().stream()
                .filter(entry -> entry.getKey() != TicketTypeRequest.Type.INFANT)
                .mapToInt(Map.Entry::getValue)
                .sum();
        seatReservationService.reserveSeat(accountId, totalSeats);
        int totalTickets = ticketCount.values().stream().mapToInt(Integer::intValue).sum();
        logger.info("Successfully reserved " + totalTickets + " tickets and " + totalSeats + " seats for Account: " + accountId);
    }

}
