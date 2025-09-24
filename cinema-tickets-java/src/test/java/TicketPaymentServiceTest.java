import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@RunWith(MockitoJUnitRunner.class)
public class TicketPaymentServiceTest {

    private static final Long accountId = 123L;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl  ticketService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    public void purchaseTicketsTest_InvalidAccountId_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Account ID must be a positive number, Received: 0");
        ticketService.purchaseTickets(0L, adultTicket);
    }

    @Test
    public void purchaseTicketsTest_NoTicketRequest_ThrowsException() throws InvalidPurchaseException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("At least one ticket type must be provided.");
        ticketService.purchaseTickets(accountId);
    }

    @Test
    public void purchaseTicketsTest_Success() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(accountId, adultTicket, childTicket, infantTicket);

        int price = TicketTypeRequest.Type.ADULT.getPrice() * 2 + TicketTypeRequest.Type.CHILD.getPrice() * 2 + TicketTypeRequest.Type.INFANT.getPrice() * 2;
        //infants dont need seat
        int seats = 2+2;
        verify(ticketPaymentService, times(1)).makePayment(accountId, price);
        verify(seatReservationService, times(1)).reserveSeat(accountId, seats);
    }

    @Test
    public void purchaseTicketsTest_MultipleTicket_Success() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        TicketTypeRequest adultTicket2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket2 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTicket2 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(accountId, adultTicket, childTicket, infantTicket, adultTicket2, childTicket2, infantTicket2);

        int price = TicketTypeRequest.Type.ADULT.getPrice() * 4 + TicketTypeRequest.Type.CHILD.getPrice() * 4 +
                TicketTypeRequest.Type.INFANT.getPrice() * 4;
        //infants dont need seat
        int seats = 4+4;
        verify(ticketPaymentService, times(1)).makePayment(accountId, price);
        verify(seatReservationService, times(1)).reserveSeat(accountId, seats);
    }

    @Test
    public void purchaseTicketsTest_AdultAndChild_Success() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        ticketService.purchaseTickets(accountId, adultTicket, childTicket);

        int price = TicketTypeRequest.Type.ADULT.getPrice() * 2 + TicketTypeRequest.Type.CHILD.getPrice() * 2;
        int seats = 2+2;
        verify(ticketPaymentService, times(1)).makePayment(accountId, price);
        verify(seatReservationService, times(1)).reserveSeat(accountId, seats);
    }

    @Test
    public void purchaseTicketsTest_AdultAndInfant_Success() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(accountId, adultTicket, infantTicket);

        int price = TicketTypeRequest.Type.ADULT.getPrice() * 2 + TicketTypeRequest.Type.INFANT.getPrice() * 2;
        //infants dont need seat
        int seats = 2;
        verify(ticketPaymentService, times(1)).makePayment(accountId, price);
        verify(seatReservationService, times(1)).reserveSeat(accountId, seats);
    }

    @Test
    public void purchaseTicketsTest_ChildAndInfantWithoutAdult_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        expectedException.expect(InvalidPurchaseException.class);
        expectedException.expectMessage("At least one adult ticket must be purchased");

        ticketService.purchaseTickets(accountId, childTicket, infantTicket);

        verify(ticketPaymentService, times(0)).makePayment(accountId, 100);
        verify(seatReservationService, times(0)).reserveSeat(accountId, 100);
    }

    @Test
    public void purchaseTicketsTest_Adult_Success() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(accountId, adultTicket);

        int price = TicketTypeRequest.Type.ADULT.getPrice() * 2;
        int seats = 2;
        verify(ticketPaymentService, times(1)).makePayment(accountId, price);
        verify(seatReservationService, times(1)).reserveSeat(accountId, seats);
    }

    @Test
    public void purchaseTicketsTest_ChildWithoutAdult_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        expectedException.expect(InvalidPurchaseException.class);
        expectedException.expectMessage("At least one adult ticket must be purchased");

        ticketService.purchaseTickets(accountId, childTicket);

        verify(ticketPaymentService, times(0)).makePayment(accountId, 100);
        verify(seatReservationService, times(0)).reserveSeat(accountId, 100);
    }

    @Test
    public void purchaseTicketsTest_InfantWithoutAdult_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        expectedException.expect(InvalidPurchaseException.class);
        expectedException.expectMessage("At least one adult ticket must be purchased");

        ticketService.purchaseTickets(accountId, infantTicket);

        verify(ticketPaymentService, times(0)).makePayment(accountId, 100);
        verify(seatReservationService, times(0)).reserveSeat(accountId, 100);
    }


    @Test
    public void purchaseTicketsTest_TotalTicketLimit_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 20);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 20);

        expectedException.expect(InvalidPurchaseException.class);
        expectedException.expectMessage("Number of total tickets must not be more than 25.");

        ticketService.purchaseTickets(accountId, adultTicket, childTicket, infantTicket);

        verify(ticketPaymentService, times(0)).makePayment(accountId, 800);
        verify(seatReservationService, times(0)).reserveSeat(accountId, 40);
    }

    @Test
    public void purchaseTicketsTest_AdultTicketLessThanInfantTicket_ThrowsException() throws InvalidPurchaseException {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 10);

        expectedException.expect(InvalidPurchaseException.class);
        expectedException.expectMessage("Number of infant tickets must not be more than number of adult tickets.");

        ticketService.purchaseTickets(accountId, adultTicket, childTicket, infantTicket);

        verify(ticketPaymentService, times(0)).makePayment(accountId, 100);
        verify(seatReservationService, times(0)).reserveSeat(accountId, 30);
    }

}
