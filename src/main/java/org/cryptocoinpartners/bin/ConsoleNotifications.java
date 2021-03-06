package org.cryptocoinpartners.bin;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptocoinpartners.command.ConsoleWriter;
import org.cryptocoinpartners.esper.annotation.When;
import org.cryptocoinpartners.schema.Book;
import org.cryptocoinpartners.schema.Fill;
import org.cryptocoinpartners.schema.Listing;
import org.cryptocoinpartners.schema.Market;
import org.cryptocoinpartners.schema.Order;
import org.cryptocoinpartners.schema.OrderUpdate;
import org.cryptocoinpartners.schema.SyntheticMarket;
import org.cryptocoinpartners.schema.Trade;

/**
 * This is attached to the Context operated by ConsoleRunMode. It is responsible for printing event alerts to the console.
 * 
 * @author Tim Olson
 */
@Singleton
public class ConsoleNotifications {

  public void watch(Listing listing) {
    if (watchList.add(listing))
      out.println("now watching " + listing);
    else
      out.println("already watching " + listing);
    out.flush();
  }

  public void unwatch(Listing listing) {
    if (watchList.remove(listing))
      out.println("no longer watching " + listing);
    out.flush();
  }

  public Set<Listing> getWatchList() {
    return watchList;
  }

  @When("select * from Book")
  private void watchBook(Book b) {
    if (!b.getMarket().isSynthetic()) {
      Market market = (Market) b.getMarket();

      if (watching(market.getListing())) {
        out.println(String.format("book: %s\t%s (%s) - %s (%s)", market, b.getBidPrice(), b.getBidVolume(), b.getAskPrice(), b.getAskVolume()));
        out.flush();
      }
    } else {
      SyntheticMarket market = (SyntheticMarket) b.getMarket();
      for (Market childMarket : market.getMarkets()) {
        if (watching(childMarket.getListing())) {
          out.println(String.format("book: %s\t%s (%s) - %s (%s)", market, b.getBidPrice(), b.getBidVolume(), b.getAskPrice(), b.getAskVolume()));
          out.flush();
        }
      }
    }
  }

  @When("select * from Trade")
  private void watchTrade(Trade t) {
    if (!t.getMarket().isSynthetic()) {
      Market market = (Market) t.getMarket();
      if (watching(market.getListing())) {
        out.println(String.format("trade: %s\t%s (%s)", market, t.getPrice(), t.getVolume()));
        out.flush();
      }
    } else {
      SyntheticMarket market = (SyntheticMarket) t.getMarket();
      for (Market childMarket : market.getMarkets()) {
        if (watching(childMarket.getListing())) {
          out.println(String.format("trade: %s\t%s (%s)", market, t.getPrice(), t.getVolume()));
          out.flush();
        }
      }
    }
  }

  @When("select * from Fill")
  private void announceFill(Fill f) {
    out.println("Filled order " + f.getOrder().getId() + ": " + f);
    out.flush();
  }

  @When("select * from OrderUpdate")
  private void announceUpdate(OrderUpdate update) {
    Order order = update.getOrder();
    switch (update.getState()) {
      case NEW:
        out.println("Creating order " + order);
        break;
      case ROUTED:
        out.println("Order has been placed. " + order);
        break;
      case PLACED:
        out.println("Order has been placed. " + order);
        break;
      case PARTFILLED:
        out.println("Order is partially filled " + order);
        break;
      case FILLED:
        out.println("Order has been completely filled.  " + order);
        break;
      case CANCELLING:
        out.println("Cancelling order " + order);
        break;
      case CANCELLED:
        out.println("Cancelled order " + order);
        break;
      case REJECTED:
        out.println("Order REJECTED as unfillable. " + order);
        break;
      case EXPIRED:
        out.println("Order has expired.  " + order);
        break;

      case ERROR:
        out.println("Order is errored.  " + order);
        break;
      default:
        out.println("Unknown order state: " + update.getState());
        break;
    }
  }

  private boolean watching(Listing listing) {
    return watchList.contains(listing);
  }

  @Inject
  private ConsoleWriter out;
  private final Set<Listing> watchList = new HashSet<>();
}
