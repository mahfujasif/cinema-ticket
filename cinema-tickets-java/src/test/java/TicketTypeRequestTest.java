import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

public class TicketTypeRequestTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void constructorTest_ZeroTicketCount_ThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Number of tickets must be positive, Received: 0");
        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
    }

    @Test
    public void constructorTest_NullTicketType_ThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Ticket type must not be null");
        new TicketTypeRequest(null, 5);
    }
}
