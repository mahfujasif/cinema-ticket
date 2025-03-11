package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */

public final class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        if(noOfTickets <= 0) {
            throw new IllegalArgumentException("Number of tickets must be positive, Received: " + noOfTickets);
        }
        if(type == null) {
            throw new IllegalArgumentException("Ticket type must not be null");
        }
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    // Adding price to Enum ensures all the newly added types have price with them
    public enum Type {
        ADULT(25),
        CHILD(15),
        INFANT(0);

        private final int price;

        Type(int price) {
            this.price = price;
        }

        public int getPrice() {
            return price;
        }
    }

}
