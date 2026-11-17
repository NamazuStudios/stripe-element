(function(React) {
  "use strict";
  var _a;
  function sessionHeaders(extra) {
    var _a2;
    const token = (_a2 = window.__elementsApiClient) == null ? void 0 : _a2.getSessionToken();
    const headers = { "Content-Type": "application/json", ...extra };
    if (token) headers["session_secret"] = token;
    return headers;
  }
  function defaultLimit() {
    var _a2;
    return ((_a2 = window.__elementsSettings) == null ? void 0 : _a2.getResultsPerPage()) ?? 20;
  }
  const CONFIG_URL = "/element/stripe/api/stripe/config";
  const EMPTY_CONFIG = { apiKey: "", webhookSecret: "" };
  const MODE_TABS = [
    { id: "production", label: "Production" },
    { id: "sandbox", label: "Sandbox" }
  ];
  function SecretField({
    label,
    hint,
    placeholder,
    value,
    onChange
  }) {
    const [revealed, setRevealed] = React.useState(false);
    const inputId = React.useId();
    return /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between" }, /* @__PURE__ */ React.createElement("label", { htmlFor: inputId, className: "text-sm font-medium" }, label), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "button",
        onClick: () => setRevealed((r) => !r),
        "aria-pressed": revealed,
        className: "text-xs text-muted-foreground hover:text-foreground transition-colors"
      },
      revealed ? "Hide" : "Show"
    )), /* @__PURE__ */ React.createElement(
      "input",
      {
        id: inputId,
        type: revealed ? "text" : "password",
        value,
        onChange: (e) => onChange(e.target.value),
        placeholder,
        autoComplete: "off",
        spellCheck: false,
        className: "w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
      }
    ), /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground" }, hint));
  }
  function ConfigFields({
    description,
    apiKeyPlaceholder,
    config,
    onChange
  }) {
    return /* @__PURE__ */ React.createElement("fieldset", { className: "space-y-5" }, /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, description), /* @__PURE__ */ React.createElement(
      SecretField,
      {
        label: "API Key",
        hint: "The Stripe secret key used for PaymentIntent and Subscription API calls.",
        placeholder: apiKeyPlaceholder,
        value: config.apiKey,
        onChange: (apiKey) => onChange({ ...config, apiKey })
      }
    ), /* @__PURE__ */ React.createElement(
      SecretField,
      {
        label: "Webhook Signing Secret",
        hint: /* @__PURE__ */ React.createElement(React.Fragment, null, "Found in the Stripe Dashboard under Developers → Webhooks. Must use the Account (not v2) webhook type."),
        placeholder: "whsec_…",
        value: config.webhookSecret,
        onChange: (webhookSecret) => onChange({ ...config, webhookSecret })
      }
    ));
  }
  function StripeConfigPlugin() {
    const [activeMode, setActiveMode] = React.useState("production");
    const [production, setProduction] = React.useState(EMPTY_CONFIG);
    const [sandbox, setSandbox] = React.useState(EMPTY_CONFIG);
    const [loading, setLoading] = React.useState(true);
    const [saving, setSaving] = React.useState(false);
    const [saved, setSaved] = React.useState(false);
    const [error, setError] = React.useState(null);
    React.useEffect(() => {
      async function loadConfig() {
        try {
          const res = await fetch(CONFIG_URL, { credentials: "include", headers: sessionHeaders() });
          if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
          const data = await res.json();
          setProduction(data.production);
          setSandbox(data.sandbox);
        } catch (e) {
          setError(e instanceof Error ? e.message : String(e));
        } finally {
          setLoading(false);
        }
      }
      loadConfig();
    }, []);
    async function handleSave(e) {
      e.preventDefault();
      setSaving(true);
      setSaved(false);
      setError(null);
      try {
        const res = await fetch(CONFIG_URL, {
          method: "PUT",
          credentials: "include",
          headers: sessionHeaders(),
          body: JSON.stringify({ production, sandbox })
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        setSaved(true);
      } catch (e2) {
        setError(e2 instanceof Error ? e2.message : String(e2));
      } finally {
        setSaving(false);
      }
    }
    if (loading) {
      return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-lg" }, /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Loading configuration…"));
    }
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-lg" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold mb-1" }, "Stripe Configuration"), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-6" }, "Credentials are stored in the database and override the Element’s default attributes. Values are hidden by default; use Show to reveal them. Both sets of credentials can be configured at once and selected per request via the header:", /* @__PURE__ */ React.createElement("code", null, "X-Stripe-Mode: (production/sandbox)"), " ", "Requests that omit the header use production if configured, sandbox otherwise."), /* @__PURE__ */ React.createElement("form", { onSubmit: handleSave, className: "space-y-6" }, /* @__PURE__ */ React.createElement("div", { className: "border-b border-border" }, /* @__PURE__ */ React.createElement("nav", { className: "flex gap-1", "aria-label": "Stripe mode tabs" }, MODE_TABS.map((tab) => /* @__PURE__ */ React.createElement(
      "button",
      {
        key: tab.id,
        type: "button",
        onClick: () => setActiveMode(tab.id),
        className: `px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeMode === tab.id ? "border-primary text-foreground" : "border-transparent text-muted-foreground hover:text-foreground hover:border-border"}`
      },
      tab.label
    )))), activeMode === "production" && /* @__PURE__ */ React.createElement(
      ConfigFields,
      {
        description: "Live-mode credentials used by default.",
        apiKeyPlaceholder: "sk_live_…",
        config: production,
        onChange: (config) => {
          setProduction(config);
          setSaved(false);
        }
      }
    ), activeMode === "sandbox" && /* @__PURE__ */ React.createElement(
      ConfigFields,
      {
        description: "Test-mode credentials, selected via X-Stripe-Mode: sandbox.",
        apiKeyPlaceholder: "sk_test_…",
        config: sandbox,
        onChange: (config) => {
          setSandbox(config);
          setSaved(false);
        }
      }
    ), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive" }, error), saved && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-green-500/50 bg-green-500/10 p-3 text-sm text-green-700 dark:text-green-400" }, "Configuration saved."), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "submit",
        disabled: saving,
        className: "rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
      },
      saving ? "Saving…" : "Save Configuration"
    )));
  }
  const MODES$1 = ["production", "sandbox"];
  const USAGE_DAYS$1 = 7;
  const CHARGE_STATUS_STYLES = {
    succeeded: "bg-green-500/10 text-green-700 dark:text-green-400",
    failed: "bg-destructive/10 text-destructive",
    pending: "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500"
  };
  const INTENT_STATUS_STYLES = {
    succeeded: "bg-green-500/10 text-green-700 dark:text-green-400",
    canceled: "bg-muted text-muted-foreground"
  };
  const SUB_STATUS_STYLES = {
    active: "bg-green-500/10 text-green-700 dark:text-green-400",
    trialing: "bg-blue-500/10 text-blue-700 dark:text-blue-400",
    past_due: "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500",
    canceled: "bg-muted text-muted-foreground"
  };
  function formatMoney(amount, currency) {
    if (amount == null) return "—";
    try {
      return new Intl.NumberFormat("en-US", { style: "currency", currency: currency ?? "USD" }).format(amount / 100);
    } catch {
      return `${amount / 100} ${currency ?? ""}`;
    }
  }
  function Badge({ value, styles }) {
    return /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${styles[value ?? ""] ?? "bg-muted text-muted-foreground"}` }, value ?? "—");
  }
  function SectionHeading({ children }) {
    return /* @__PURE__ */ React.createElement("h2", { className: "text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2" }, children);
  }
  function StripeBillingPlugin() {
    const limit = defaultLimit();
    const [customerId, setCustomerId] = React.useState("");
    const [mode, setMode] = React.useState("production");
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    const [searched, setSearched] = React.useState(false);
    const [paymentMethods, setPaymentMethods] = React.useState(null);
    const [upcoming, setUpcoming] = React.useState(null);
    const [charges, setCharges] = React.useState(null);
    const [invoices, setInvoices] = React.useState(null);
    const [intents, setIntents] = React.useState(null);
    const [usage, setUsage] = React.useState(null);
    const [subscriptions, setSubscriptions] = React.useState(null);
    const [portalLoading, setPortalLoading] = React.useState(false);
    const [portalUrl, setPortalUrl] = React.useState(null);
    const [copied, setCopied] = React.useState(false);
    async function fetchBilling(selectedMode, id) {
      const headers = { ...sessionHeaders(), "X-Stripe-Mode": selectedMode };
      const enc = encodeURIComponent(id.trim());
      async function getJson(path) {
        const res = await fetch(path, { credentials: "include", headers });
        if (res.status === 204) return null;
        if (!res.ok) {
          console.warn(`GET ${path} failed: ${res.status} ${res.statusText}`);
          return null;
        }
        return res.json();
      }
      async function fetchUsage() {
        const meters = await getJson("/element/stripe/api/stripe/meters?active=true&limit=100");
        if (!(meters == null ? void 0 : meters.length)) return {};
        const end = /* @__PURE__ */ new Date();
        const start = new Date(end.getTime() - USAGE_DAYS$1 * 24 * 3600 * 1e3);
        const range = `&start=${start.toISOString().slice(0, 17)}00Z&end=${end.toISOString().slice(0, 17)}00Z`;
        const totals = {};
        await Promise.all(meters.map(async (m) => {
          const events = await getJson(
            `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${enc}${range}`
          );
          if (events) {
            totals[m.eventName] = events.reduce((sum, s) => sum + parseFloat(s.aggregatedValue ?? "0"), 0);
          }
        }));
        return totals;
      }
      const [methods, upcomingInvoice, chargeList, invoiceList, intentList, usageTotals, subs] = await Promise.all([
        getJson(`/element/stripe/api/stripe/customer/${enc}/payment-methods`),
        getJson(`/element/stripe/api/stripe/customer/${enc}/upcoming-invoice`),
        // null (204) = nothing scheduled
        getJson(`/element/stripe/api/stripe/customer/${enc}/charges?limit=${Math.min(limit, 100)}`),
        getJson(`/element/stripe/api/stripe/customer/${enc}/invoices?limit=${Math.min(limit, 100)}`),
        getJson(`/element/stripe/api/stripe/customer/${enc}/payment-intents?limit=${Math.min(limit, 100)}`),
        fetchUsage(),
        getJson(`/element/stripe/api/stripe/customer/${enc}/subscriptions?limit=5`).then((r) => (r == null ? void 0 : r.subscriptions) ?? null)
      ]);
      setPaymentMethods(methods);
      setUpcoming(upcomingInvoice ?? "none");
      setCharges(chargeList);
      setInvoices(invoiceList);
      setIntents(intentList);
      setUsage(usageTotals);
      setSubscriptions(subs);
    }
    async function handleSearch(e) {
      e.preventDefault();
      if (!customerId.trim()) return;
      setLoading(true);
      setError(null);
      setSearched(false);
      setPortalUrl(null);
      try {
        await fetchBilling(mode, customerId);
        setSearched(true);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setLoading(false);
      }
    }
    async function handleGetPortalLink() {
      setPortalLoading(true);
      setError(null);
      setPortalUrl(null);
      setCopied(false);
      try {
        const res = await fetch(`/element/stripe/api/stripe/customer/${encodeURIComponent(customerId.trim())}/portal-session`, {
          method: "POST",
          credentials: "include",
          headers: { ...sessionHeaders(), "X-Stripe-Mode": mode }
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        setPortalUrl((await res.json()).url);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setPortalLoading(false);
      }
    }
    function handleCopy() {
      if (!portalUrl) return;
      navigator.clipboard.writeText(portalUrl).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2e3);
      });
    }
    const onFile = paymentMethods && paymentMethods.length > 0 ? paymentMethods.filter((m) => m.brand).map((m) => `${m.brand.toUpperCase()} ••${m.last4 ?? ""}`).join(", ") : null;
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-5xl" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold mb-1" }, "Stripe Billing"), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-6" }, "Full billing posture for a customer: charges and payment attempts (one-time purchases), pending metered usage and the upcoming invoice, invoices, and subscriptions."), /* @__PURE__ */ React.createElement("form", { onSubmit: handleSearch, className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5 flex-1 min-w-48" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "billing-customer-id", className: "block text-sm font-medium" }, "Customer ID"), /* @__PURE__ */ React.createElement(
      "input",
      {
        id: "billing-customer-id",
        type: "text",
        value: customerId,
        onChange: (e) => setCustomerId(e.target.value),
        placeholder: "cus_...",
        className: "w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
      }
    )), /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "billing-mode", className: "block text-sm font-medium" }, "Mode"), /* @__PURE__ */ React.createElement(
      "select",
      {
        id: "billing-mode",
        value: mode,
        onChange: (e) => setMode(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      MODES$1.map((m) => /* @__PURE__ */ React.createElement("option", { key: m, value: m }, m === "production" ? "Production (live)" : "Sandbox (test)"))
    )), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "submit",
        disabled: loading || !customerId.trim(),
        className: "rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
      },
      loading ? "Searching…" : "Search"
    ), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "button",
        onClick: handleGetPortalLink,
        disabled: portalLoading || !customerId.trim(),
        className: "rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      portalLoading ? "Getting link…" : "Get Portal Link"
    )), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), portalUrl && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border bg-muted/20 p-3 mb-4 flex items-center gap-3" }, /* @__PURE__ */ React.createElement("span", { className: "text-xs font-mono truncate flex-1 text-muted-foreground" }, portalUrl), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "button",
        onClick: handleCopy,
        className: "shrink-0 rounded px-2 py-1 text-xs border border-border hover:bg-muted/40 transition-colors"
      },
      copied ? "Copied!" : "Copy"
    )), searched && customerId.trim() && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6" }, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Payment method"), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, paymentMethods === null ? "—" : onFile ?? "None on file")), /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Upcoming invoice"), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, upcoming === "none" ? "Nothing scheduled" : upcoming ? `${formatMoney(upcoming.total, upcoming.currency)} · due ${upcoming.periodEnd ? new Date(upcoming.periodEnd).toLocaleDateString() : "—"}` : "—"), upcoming && upcoming !== "none" && upcoming.lines.length > 0 && /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mt-1 truncate", title: upcoming.lines.map((l) => l.description ?? "").join("; ") }, upcoming.lines.length, " line item", upcoming.lines.length === 1 ? "" : "s")), /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Metered usage, last ", USAGE_DAYS$1, " days"), usage === null ? /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, "—") : Object.keys(usage).length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, "No active meters") : Object.entries(usage).map(([event, total]) => /* @__PURE__ */ React.createElement("p", { key: event, className: "text-sm font-medium" }, Number.isInteger(total) ? total : total.toFixed(4), " ", /* @__PURE__ */ React.createElement("span", { className: "text-xs text-muted-foreground font-normal" }, event))))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Charges (one-time purchases)"), charges === null ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Failed to load.") : charges.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No charges.") : /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Date"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Amount"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Status"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Description"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Charge"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Receipt"))), /* @__PURE__ */ React.createElement("tbody", null, charges.map((ch, i) => /* @__PURE__ */ React.createElement("tr", { key: ch.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, ch.created ? new Date(ch.created).toLocaleDateString() : "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(ch.amount, ch.currency), ch.refundedAmount > 0 && !ch.refunded && /* @__PURE__ */ React.createElement("span", { className: "text-xs text-orange-600 ml-1" }, "(−", formatMoney(ch.refundedAmount, ch.currency), ")")), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: ch.refunded ? "refunded" : ch.status, styles: { ...CHARGE_STATUS_STYLES, refunded: "bg-orange-500/10 text-orange-600" } })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs max-w-xs truncate" }, ch.description ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, ch.id), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, ch.receiptUrl ? /* @__PURE__ */ React.createElement("a", { href: ch.receiptUrl, target: "_blank", rel: "noreferrer", className: "text-primary hover:underline" }, "Receipt ↗") : "—"))))))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Invoices"), invoices === null ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Failed to load.") : invoices.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No invoices.") : /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Date"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Amount paid"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Status"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Invoice"))), /* @__PURE__ */ React.createElement("tbody", null, invoices.map((inv, i) => /* @__PURE__ */ React.createElement("tr", { key: inv.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, new Date(inv.created).toLocaleDateString()), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(inv.amountPaid, inv.currency)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: inv.status, styles: {} })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, inv.id))))))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Payment attempts"), intents === null ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Failed to load.") : intents.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No PaymentIntents.") : /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Date"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Amount"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Status"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Intent"))), /* @__PURE__ */ React.createElement("tbody", null, intents.map((pi, i) => /* @__PURE__ */ React.createElement("tr", { key: pi.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, pi.created ? new Date(pi.created).toLocaleDateString() : "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(pi.amount, pi.currency)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: pi.status, styles: INTENT_STATUS_STYLES })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, pi.id))))))), /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement(SectionHeading, null, "Subscriptions"), subscriptions === null ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Failed to load.") : subscriptions.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No subscriptions.") : /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Subscription"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Status"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Renews"))), /* @__PURE__ */ React.createElement("tbody", null, subscriptions.map((s, i) => /* @__PURE__ */ React.createElement("tr", { key: s.subscriptionId, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, s.subscriptionId), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: s.status, styles: SUB_STATUS_STYLES })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs" }, s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : "—")))))))));
  }
  const MODES = ["production", "sandbox"];
  const USAGE_DAYS = 7;
  function stripeDashboardUrl(mode, path) {
    return mode === "sandbox" ? `https://dashboard.stripe.com/test${path}` : `https://dashboard.stripe.com${path}`;
  }
  function displayName(c) {
    return c.name || c.metadata["orgName"] || c.metadata["name"] || c.email || "—";
  }
  function StripeCustomersPlugin() {
    const limit = defaultLimit();
    const [mode, setMode] = React.useState("production");
    const [customers, setCustomers] = React.useState(null);
    const [meters, setMeters] = React.useState([]);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    const [expanded, setExpanded] = React.useState(null);
    const [copied, setCopied] = React.useState(null);
    const [cursorStack, setCursorStack] = React.useState([]);
    const [hasMore, setHasMore] = React.useState(false);
    async function fetchCustomers(selectedMode, startingAfter, nextStack) {
      setLoading(true);
      setError(null);
      setExpanded(null);
      try {
        const params = new URLSearchParams({ limit: String(Math.max(limit, 100)) });
        if (startingAfter) params.set("startingAfter", startingAfter);
        const res = await fetch(`/element/stripe/api/stripe/customers?${params}`, {
          credentials: "include",
          headers: { ...sessionHeaders(), "X-Stripe-Mode": selectedMode }
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        const data = await res.json();
        setCustomers(data.customers);
        setHasMore(data.hasMore);
        setCursorStack(nextStack);
        const metersRes = await fetch("/element/stripe/api/stripe/meters?active=true&limit=100", {
          credentials: "include",
          headers: { ...sessionHeaders(), "X-Stripe-Mode": selectedMode }
        });
        if (metersRes.ok) setMeters(await metersRes.json());
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }
    const currentCursor = cursorStack.length ? cursorStack[cursorStack.length - 1] : null;
    React.useEffect(() => {
      fetchCustomers(mode, null, []);
    }, [mode]);
    function refreshCurrentPage() {
      fetchCustomers(mode, currentCursor, cursorStack);
    }
    function fetchNext() {
      if (!(customers == null ? void 0 : customers.length)) return;
      fetchCustomers(mode, customers[customers.length - 1].id, [...cursorStack, customers[customers.length - 1].id]);
    }
    function fetchPrevious() {
      if (!cursorStack.length) return;
      const prevStack = cursorStack.slice(0, -1);
      fetchCustomers(mode, prevStack.length ? prevStack[prevStack.length - 1] : null, prevStack);
    }
    function handleCopy(id) {
      navigator.clipboard.writeText(id).then(() => {
        setCopied(id);
        setTimeout(() => setCopied(null), 2e3);
      });
    }
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-6xl" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between mb-1" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold" }, "Customers / Billing"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: refreshCurrentPage,
        disabled: loading,
        className: "rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      loading ? "Refreshing…" : "Refresh"
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-4" }, "Every customer on this Stripe account in the selected mode — each one is billing-enabled there. Expand a row for its recent metered usage (last ", USAGE_DAYS, " days, per meter, from Stripe's aggregates) and subscriptions. Cross-check posting decisions in the Postings tab."), /* @__PURE__ */ React.createElement("div", { className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "block text-sm font-medium" }, "Mode"), /* @__PURE__ */ React.createElement(
      "select",
      {
        value: mode,
        onChange: (e) => setMode(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      MODES.map((m) => /* @__PURE__ */ React.createElement("option", { key: m, value: m }, m === "production" ? "Production (live)" : "Sandbox (test)"))
    ))), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), customers && customers.length === 0 && /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No customers exist in ", mode, " mode yet."), customers && customers.length > 0 && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm table-fixed" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground w-[18rem]" }, "Customer ID"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Name"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Email"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Created"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Stripe"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2" }))), /* @__PURE__ */ React.createElement("tbody", null, customers.map((c, i) => {
      const isExpanded = expanded === c.id;
      return /* @__PURE__ */ React.createElement(React.Fragment, { key: c.id }, /* @__PURE__ */ React.createElement("tr", { className: `${i % 2 === 0 ? "" : "bg-muted/20"} ${isExpanded ? "bg-muted/40" : ""}` }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-2" }, /* @__PURE__ */ React.createElement(
        "button",
        {
          onClick: () => handleCopy(c.id),
          title: "Copy customer ID",
          className: "truncate max-w-[12rem] text-left hover:underline"
        },
        c.id
      ), /* @__PURE__ */ React.createElement("span", { className: "w-14 shrink-0 text-[10px] text-green-600" }, copied === c.id ? "copied!" : ""))), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 truncate" }, displayName(c)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs truncate" }, c.email ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, c.created ? new Date(c.created).toLocaleDateString() : "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs whitespace-nowrap" }, /* @__PURE__ */ React.createElement(
        "a",
        {
          href: stripeDashboardUrl(mode, `/customers/${c.id}`),
          target: "_blank",
          rel: "noreferrer",
          className: "text-primary hover:underline"
        },
        "Dashboard ↗"
      ), /* @__PURE__ */ React.createElement("span", { className: "mx-1 text-muted-foreground" }, "·"), /* @__PURE__ */ React.createElement(
        "a",
        {
          href: stripeDashboardUrl(mode, "/billing/meters"),
          target: "_blank",
          rel: "noreferrer",
          className: "text-primary hover:underline"
        },
        "Meters ↗"
      )), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-right" }, /* @__PURE__ */ React.createElement(
        "button",
        {
          onClick: () => setExpanded(isExpanded ? null : c.id),
          className: "rounded-md border border-border px-2 py-1 text-xs hover:bg-muted/40 transition-colors"
        },
        isExpanded ? "Hide" : "Usage"
      ))), isExpanded && /* @__PURE__ */ React.createElement("tr", null, /* @__PURE__ */ React.createElement("td", { colSpan: 6, className: "px-4 py-3 bg-muted/10" }, /* @__PURE__ */ React.createElement(CustomerDetail, { customerId: c.id, mode, meters }))));
    })))), /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-3 text-sm mt-4" }, /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: fetchPrevious,
        disabled: loading || cursorStack.length === 0,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "← Previous"
    ), /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, cursorStack.length > 0 ? `Page ${cursorStack.length + 1}` : "Page 1"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: fetchNext,
        disabled: loading || !hasMore,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "Next →"
    ))));
  }
  function CustomerDetail({ customerId, mode, meters }) {
    const [usage, setUsage] = React.useState({});
    const [subscriptions, setSubscriptions] = React.useState(null);
    const [loading, setLoading] = React.useState(true);
    React.useEffect(() => {
      let cancelled = false;
      async function load() {
        setLoading(true);
        const headers = { ...sessionHeaders(), "X-Stripe-Mode": mode };
        const end = /* @__PURE__ */ new Date();
        const start = new Date(end.getTime() - USAGE_DAYS * 24 * 3600 * 1e3);
        const range = `&start=${start.toISOString().slice(0, 17)}00Z&end=${end.toISOString().slice(0, 17)}00Z`;
        const usageResult = {};
        await Promise.all(meters.map(async (m) => {
          try {
            const res = await fetch(
              `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${encodeURIComponent(customerId)}${range}`,
              { credentials: "include", headers }
            );
            usageResult[m.eventName] = res.ok ? await res.json() : "error";
          } catch {
            usageResult[m.eventName] = "error";
          }
        }));
        if (cancelled) return;
        setUsage(usageResult);
        try {
          const res = await fetch(
            `/element/stripe/api/stripe/customer/${encodeURIComponent(customerId)}/subscriptions?limit=5`,
            { credentials: "include", headers }
          );
          if (res.ok && !cancelled) setSubscriptions((await res.json()).subscriptions);
        } catch {
        }
        if (!cancelled) setLoading(false);
      }
      load();
      return () => {
        cancelled = true;
      };
    }, [customerId, mode, meters]);
    const hasUsage = Object.values(usage).some((u) => Array.isArray(u) && u.length > 0);
    return /* @__PURE__ */ React.createElement("div", { className: "text-xs space-y-3" }, /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement("p", { className: "font-medium mb-1 text-muted-foreground" }, "Metered usage, last ", USAGE_DAYS, " days (Stripe aggregates)"), meters.length === 0 ? /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, "No active meters on this account.") : loading ? /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, "Loading…") : !hasUsage ? /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, "No usage recorded for this customer in the window.") : /* @__PURE__ */ React.createElement("table", { className: "w-full max-w-lg" }, /* @__PURE__ */ React.createElement("tbody", null, meters.map((m) => {
      const u = usage[m.eventName];
      const total = Array.isArray(u) ? u.reduce((sum, s) => sum + parseFloat(s.aggregatedValue ?? "0"), 0) : null;
      return /* @__PURE__ */ React.createElement("tr", { key: m.eventName }, /* @__PURE__ */ React.createElement("td", { className: "py-0.5 pr-4 font-mono" }, m.eventName), /* @__PURE__ */ React.createElement("td", { className: "py-0.5 text-right font-medium" }, total === null ? /* @__PURE__ */ React.createElement("span", { className: "text-destructive" }, "error") : Number.isInteger(total) ? total : total.toFixed(4)));
    })))), subscriptions && subscriptions.length > 0 && /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement("p", { className: "font-medium mb-1 text-muted-foreground" }, "Subscriptions"), subscriptions.map((s) => /* @__PURE__ */ React.createElement("p", { key: s.subscriptionId, className: "font-mono" }, s.subscriptionId, " · ", /* @__PURE__ */ React.createElement("span", null, s.status), s.currentPeriodEnd && /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, " · renews ", new Date(s.currentPeriodEnd).toLocaleDateString())))));
  }
  const EVENT_TYPE_OPTIONS = [
    { value: "", label: "All types" },
    { value: "payment_intent.succeeded", label: "Payment succeeded" },
    { value: "payment_intent.payment_failed", label: "Payment failed" },
    { value: "invoice.payment_succeeded", label: "Invoice paid" },
    { value: "invoice.payment_failed", label: "Invoice payment failed" },
    { value: "customer.subscription.created", label: "Subscription created" },
    { value: "customer.subscription.updated", label: "Subscription updated" },
    { value: "customer.subscription.deleted", label: "Subscription canceled" },
    { value: "customer.subscription.trial_will_end", label: "Trial ending soon" }
  ];
  const EVENT_TYPE_STYLES = {
    "payment_intent.succeeded": "bg-green-500/10 text-green-700 dark:text-green-400",
    "invoice.payment_succeeded": "bg-green-500/10 text-green-700 dark:text-green-400",
    "payment_intent.payment_failed": "bg-destructive/10 text-destructive",
    "invoice.payment_failed": "bg-destructive/10 text-destructive",
    "customer.subscription.deleted": "bg-muted text-muted-foreground",
    "customer.subscription.trial_will_end": "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500"
  };
  function StripeEventLogPlugin() {
    const limit = defaultLimit();
    const [type, setType] = React.useState("");
    const [offset, setOffset] = React.useState(0);
    const [result, setResult] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    async function fetchEvents(newOffset) {
      setLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams({ limit: String(limit), offset: String(newOffset) });
        if (type) params.set("type", type);
        const res = await fetch(`/element/stripe/api/stripe/events?${params}`, {
          credentials: "include",
          headers: sessionHeaders()
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        setResult(await res.json());
        setOffset(newOffset);
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }
    React.useEffect(() => {
      fetchEvents(0);
    }, [type]);
    const page = Math.floor(offset / limit) + 1;
    const totalPages = result ? Math.ceil(result.total / limit) : 1;
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-4xl" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between mb-1" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold" }, "Stripe Webhook Events"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchEvents(offset),
        disabled: loading,
        className: "rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      loading ? "Refreshing…" : "Refresh"
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-6" }, "Verified webhook events received by this Element, newest first."), /* @__PURE__ */ React.createElement("div", { className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "text-sm font-medium" }, "Event type"), /* @__PURE__ */ React.createElement(
      "select",
      {
        value: type,
        onChange: (e) => setType(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      EVENT_TYPE_OPTIONS.map((o) => /* @__PURE__ */ React.createElement("option", { key: o.value, value: o.value }, o.label))
    ))), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), result && (result.events.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No events found.") : /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-hidden mb-4" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Event ID"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Type"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Received"))), /* @__PURE__ */ React.createElement("tbody", null, result.events.map((evt, i) => /* @__PURE__ */ React.createElement("tr", { key: evt.stripeEventId, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, evt.stripeEventId), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${EVENT_TYPE_STYLES[evt.eventType] ?? "bg-muted text-muted-foreground"}` }, evt.eventType)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs" }, new Date(evt.receivedAt).toLocaleString())))))), /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-3 text-sm" }, /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchEvents(offset - limit),
        disabled: loading || offset === 0,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "← Previous"
    ), /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, "Page ", page, " of ", totalPages, " · ", result.total, " total"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchEvents(offset + limit),
        disabled: loading || !result.hasMore,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "Next →"
    )))));
  }
  const MODE_OPTIONS = [
    { value: "both", label: "Both modes" },
    { value: "sandbox", label: "Sandbox" },
    { value: "production", label: "Production" }
  ];
  const OUTCOME_OPTIONS = [
    { value: "", label: "All outcomes" },
    { value: "POSTED", label: "Posted" },
    { value: "FAILED", label: "Failed" },
    { value: "SKIPPED_EXEMPT", label: "Skipped (exempt)" },
    { value: "SKIPPED_NO_CUSTOMER", label: "Skipped (no customer)" },
    { value: "RECOVERED_STALE_PENDING", label: "Recovered stale pending" }
  ];
  const OUTCOME_STYLES = {
    POSTED: "bg-green-500/10 text-green-700 dark:text-green-400",
    RECOVERED_STALE_PENDING: "bg-blue-500/10 text-blue-700 dark:text-blue-400",
    FAILED: "bg-destructive/10 text-destructive",
    SKIPPED_EXEMPT: "bg-muted text-muted-foreground",
    SKIPPED_NO_CUSTOMER: "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500"
  };
  function StripePostingPlugin() {
    const limit = defaultLimit();
    const [mode, setMode] = React.useState("both");
    const [outcome, setOutcome] = React.useState("");
    const [customer, setCustomer] = React.useState("");
    const [offset, setOffset] = React.useState(0);
    const [result, setResult] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    async function fetchPostings(newOffset) {
      setLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams({ limit: String(limit), offset: String(newOffset), mode });
        if (outcome) params.set("outcome", outcome);
        if (customer.trim()) params.set("customer", customer.trim());
        const res = await fetch(`/element/stripe/api/stripe/postings?${params}`, {
          credentials: "include",
          headers: sessionHeaders()
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        setResult(await res.json());
        setOffset(newOffset);
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }
    React.useEffect(() => {
      fetchPostings(0);
    }, [mode, outcome, customer]);
    const page = Math.floor(offset / limit) + 1;
    const totalPages = result ? Math.ceil(result.total / limit) : 1;
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-6xl" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between mb-1" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold" }, "Meter Event Postings"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchPostings(offset),
        disabled: loading,
        className: "rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      loading ? "Refreshing…" : "Refresh"
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-6" }, "What each billing meter-event posting attempt actually did, as recorded by consumer elements at reconciliation time. Posted records show the idempotency key their Stripe-side usage can be verified against (see the Customers tab); failed and skipped records surface their reason here."), /* @__PURE__ */ React.createElement("div", { className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "block text-sm font-medium" }, "Mode"), /* @__PURE__ */ React.createElement(
      "select",
      {
        value: mode,
        onChange: (e) => setMode(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      MODE_OPTIONS.map((o) => /* @__PURE__ */ React.createElement("option", { key: o.value, value: o.value }, o.label))
    )), /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "block text-sm font-medium" }, "Outcome"), /* @__PURE__ */ React.createElement(
      "select",
      {
        value: outcome,
        onChange: (e) => setOutcome(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      OUTCOME_OPTIONS.map((o) => /* @__PURE__ */ React.createElement("option", { key: o.value, value: o.value }, o.label))
    )), /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "block text-sm font-medium" }, "Customer ID"), /* @__PURE__ */ React.createElement(
      "input",
      {
        value: customer,
        onChange: (e) => setCustomer(e.target.value),
        placeholder: "cus_... (exact match)",
        className: "w-64 rounded-md border border-border bg-background px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-ring"
      }
    ))), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), result && (result.records.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No posting records found.") : /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto mb-4" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Time"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Outcome"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Mode"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Identifier"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Customer"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Event"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-right font-medium text-muted-foreground" }, "Qty"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Detail"))), /* @__PURE__ */ React.createElement("tbody", null, result.records.map((rec, i) => {
      var _a2;
      return /* @__PURE__ */ React.createElement("tr", { key: `${rec.identifier}-${rec.at}-${i}`, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, new Date(rec.at).toLocaleString()), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${OUTCOME_STYLES[rec.outcome ?? ""] ?? "bg-muted text-muted-foreground"}` }, rec.outcome ?? "?")), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, ((_a2 = rec.mode) == null ? void 0 : _a2.toLowerCase()) ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, rec.identifier), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, rec.customerId ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, rec.eventName ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-right" }, rec.quantity ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-muted-foreground max-w-xs truncate", title: rec.detail ?? "" }, rec.detail ?? ""));
    })))), /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-3 text-sm" }, /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchPostings(offset - limit),
        disabled: loading || offset === 0,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "← Previous"
    ), /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, "Page ", page, " of ", totalPages, " · ", result.total, " total"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchPostings(offset + limit),
        disabled: loading || !result.hasMore,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "Next →"
    )))));
  }
  const TABS = [
    { id: "config", label: "Configuration" },
    { id: "billing", label: "Billing" },
    { id: "customers", label: "Customers" },
    { id: "postings", label: "Postings" },
    { id: "events", label: "Events" }
  ];
  function StripePlugin() {
    const [activeTab, setActiveTab] = React.useState("config");
    return /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement("div", { className: "border-b border-border px-6" }, /* @__PURE__ */ React.createElement("nav", { className: "flex gap-1", "aria-label": "Stripe tabs" }, TABS.map((tab) => /* @__PURE__ */ React.createElement(
      "button",
      {
        key: tab.id,
        onClick: () => setActiveTab(tab.id),
        className: `px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === tab.id ? "border-primary text-foreground" : "border-transparent text-muted-foreground hover:text-foreground hover:border-border"}`
      },
      tab.label
    )))), activeTab === "config" && /* @__PURE__ */ React.createElement(StripeConfigPlugin, null), activeTab === "billing" && /* @__PURE__ */ React.createElement(StripeBillingPlugin, null), activeTab === "customers" && /* @__PURE__ */ React.createElement(StripeCustomersPlugin, null), activeTab === "postings" && /* @__PURE__ */ React.createElement(StripePostingPlugin, null), activeTab === "events" && /* @__PURE__ */ React.createElement(StripeEventLogPlugin, null));
  }
  (_a = window.__elementsPlugins) == null ? void 0 : _a.register("stripe", StripePlugin);
})(window.React);
