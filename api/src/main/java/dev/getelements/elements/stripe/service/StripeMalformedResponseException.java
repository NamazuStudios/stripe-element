package dev.getelements.elements.stripe.service;

/**
 * Thrown when the underlying {@code stripe-java} library fails to parse a Stripe HTTP response
 * with a raw, non-{@code StripeException} {@code RuntimeException} — most
 * commonly a {@code ClassCastException} thrown by {@code LiveStripeResponseGetter.handleApiError}'s
 * {@code Gson.fromJson(body, JsonObject.class)} call, whose surrounding exception table only
 * catches {@code JsonSyntaxException}, not {@code ClassCastException}. This happens when Stripe
 * returns a response whose body isn't shaped as a JSON object (e.g. an array or bare scalar) for a
 * request stripe-java otherwise expects a standard {@code {"error": {...}}} shape from — the raw
 * cast exception carries no message and, once it recurs frequently enough for the JVM's "fast
 * throw" optimization to kick in, often no stack trace either, making it otherwise undiagnosable.
 *
 * <p>Observed in production for {@link StripeService#recordMeterEvent} specifically when Stripe
 * rejects an {@code event_name} with no matching active meter in the target account — the working
 * (but not yet confirmed against a captured raw response) theory is that this error condition's
 * body isn't shaped like Stripe's other error responses. Until confirmed, treat this the same as a
 * {@link NoSuchMeterException} would be treated: check whether a meter with the given event name
 * exists and is active in the target Stripe account/mode.
 */
public class StripeMalformedResponseException extends RuntimeException {

    public StripeMalformedResponseException(String message, Throwable cause) {
        super(message, cause);
    }

}
