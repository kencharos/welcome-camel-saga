package saga.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.security.SecureRandom;

@ApplicationScoped
@Named("ServiceA")
public class ServiceA {

    private final SecureRandom rnd = new SecureRandom();

    public InputB perform(String transactionContext, InputA body) {

        var price = rnd.nextInt(1000);
        System.out.printf("ServiceA: LRA-transaction(%s), %s issue price %d\n", transactionContext, body.title(), price);

        // do persist process

        return new InputB(body.title(), price);
    }

    public void cancel(String transactionContext) {
        System.out.printf("\"ServiceA: LRA-transaction(%s) do cancel..\n", transactionContext);
        // do cancel process
    }
}
