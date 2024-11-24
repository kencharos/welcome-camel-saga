package saga.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("ServiceB")
public class ServiceB {

    public void perform(String transactionContext, InputB body) {

        System.out.printf("ServiceB: LRA-transaction(%s), %s price %d\n", transactionContext, body.title(), body.price());

        if (body.price() < 500) {
            throw new IllegalStateException("price must greater 500 transaction(" + transactionContext + ")");
        }
        // do persist
    }

    public void cancel(String transactionContext) {
        System.out.printf("\"ServiceB: LRA-transaction(%s) do cancel..\n", transactionContext);
        // do cancel process
    }
}
