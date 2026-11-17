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
  const MODES = ["production", "sandbox"];
  const USAGE_DAYS = 7;
  function stripeDashboardUrl(mode, path) {
    return mode === "sandbox" ? `https://dashboard.stripe.com/test${path}` : `https://dashboard.stripe.com${path}`;
  }
  function formatMoney(amount, currency) {
    if (amount == null) return "—";
    try {
      return new Intl.NumberFormat("en-US", { style: "currency", currency: currency ?? "USD" }).format(amount / 100);
    } catch {
      return `${amount / 100} ${currency ?? ""}`;
    }
  }
  function usageRangeParams() {
    const end = /* @__PURE__ */ new Date();
    const start = new Date(end.getTime() - USAGE_DAYS * 24 * 3600 * 1e3);
    const floor = (d) => d.toISOString().slice(0, 17) + "00Z";
    return `&start=${floor(start)}&end=${floor(end)}`;
  }
  function sumUsage(events) {
    return events.reduce((sum, e) => sum + parseFloat(e.aggregatedValue ?? "0"), 0);
  }
  function useCustomerBilling(customerId, mode, meters) {
    const [loading, setLoading] = React.useState(true);
    const [paymentMethods, setPaymentMethods] = React.useState(null);
    const [upcoming, setUpcoming] = React.useState(null);
    const [charges, setCharges] = React.useState(null);
    const [invoices, setInvoices] = React.useState(null);
    const [intents, setIntents] = React.useState(null);
    const [usage, setUsage] = React.useState({});
    const [subscriptions, setSubscriptions] = React.useState(null);
    React.useEffect(() => {
      let cancelled = false;
      async function load() {
        setLoading(true);
        const headers = { ...sessionHeaders(), "X-Stripe-Mode": mode };
        const enc = encodeURIComponent(customerId);
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
          const result = {};
          await Promise.all(meters.map(async (m) => {
            try {
              const res = await fetch(
                `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${enc}${usageRangeParams()}&window=hour`,
                { credentials: "include", headers }
              );
              result[m.eventName] = res.ok ? await res.json() : "error";
            } catch {
              result[m.eventName] = "error";
            }
          }));
          return result;
        }
        const [methods, upcomingInvoice, chargeList, invoiceList, intentList, usageResult, subs] = await Promise.all([
          getJson(`/element/stripe/api/stripe/customer/${enc}/payment-methods`),
          getJson(`/element/stripe/api/stripe/customer/${enc}/upcoming-invoice`),
          getJson(`/element/stripe/api/stripe/customer/${enc}/charges?limit=10`),
          getJson(`/element/stripe/api/stripe/customer/${enc}/invoices?limit=10`),
          getJson(`/element/stripe/api/stripe/customer/${enc}/payment-intents?limit=10`),
          fetchUsage(),
          getJson(`/element/stripe/api/stripe/customer/${enc}/subscriptions?limit=5`).then((r) => (r == null ? void 0 : r.subscriptions) ?? null)
        ]);
        if (cancelled) return;
        setPaymentMethods(methods);
        setUpcoming(upcomingInvoice ?? "none");
        setCharges(chargeList);
        setInvoices(invoiceList);
        setIntents(intentList);
        setUsage(usageResult);
        setSubscriptions(subs);
        setLoading(false);
      }
      load();
      return () => {
        cancelled = true;
      };
    }, [customerId, mode, meters]);
    const usageByDay = React.useMemo(() => {
      const map = {};
      for (const [event, events] of Object.entries(usage)) {
        map[event] = {};
        if (Array.isArray(events)) {
          for (const e of events) {
            if (!e.startTime) continue;
            const key = new Date(e.startTime).toDateString();
            map[event][key] = (map[event][key] ?? 0) + parseFloat(e.aggregatedValue ?? "0");
          }
        }
      }
      return map;
    }, [usage]);
    const usageWeekTotals = React.useMemo(() => {
      const totals = {};
      for (const [event] of Object.entries(usage)) {
        totals[event] = sumUsage(Array.isArray(usage[event]) ? usage[event] : []);
      }
      return totals;
    }, [usage]);
    return { loading, paymentMethods, upcoming, charges, invoices, intents, subscriptions, usage, usageByDay, usageWeekTotals };
  }
  const OUTCOME_STYLES$1 = {
    POSTED: "bg-green-500/10 text-green-700 dark:text-green-400",
    RECOVERED_STALE_PENDING: "bg-blue-500/10 text-blue-700 dark:text-blue-400",
    FAILED: "bg-destructive/10 text-destructive",
    SKIPPED_EXEMPT: "bg-muted text-muted-foreground",
    SKIPPED_NO_CUSTOMER: "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500"
  };
  function CustomerPostingsSection({ customerId }) {
    const limit = defaultLimit();
    const [offset, setOffset] = React.useState(0);
    const [result, setResult] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    async function fetchPostings(newOffset) {
      setLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams({ customer: customerId, limit: String(limit), offset: String(newOffset) });
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
    }, [customerId]);
    return /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement("h2", { className: "text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2" }, "Posting ledger"), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-3" }, error), result && result.records.length === 0 && /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No posting records for this customer. Consumer elements only record skips here — postings made via recordMeterEvent are recorded automatically; an empty ledger with no failed charges usually means nothing was posted or skipped for this customer."), result && result.records.length > 0 && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto mb-3" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Time"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Outcome"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Mode"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Identifier"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Event"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-right font-medium text-muted-foreground" }, "Qty"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Detail"))), /* @__PURE__ */ React.createElement("tbody", null, result.records.map((rec, i) => {
      var _a2;
      return /* @__PURE__ */ React.createElement("tr", { key: `${rec.identifier}-${rec.at}-${i}`, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, new Date(rec.at).toLocaleString()), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${OUTCOME_STYLES$1[rec.outcome ?? ""] ?? "bg-muted text-muted-foreground"}` }, rec.outcome ?? "?")), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, ((_a2 = rec.mode) == null ? void 0 : _a2.toLowerCase()) ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, rec.identifier), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, rec.eventName ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-right" }, rec.quantity ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-muted-foreground max-w-xs truncate", title: rec.detail ?? "" }, rec.detail ?? ""));
    })))), /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-3 text-sm" }, /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchPostings(offset - limit),
        disabled: loading || offset === 0,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "← Previous"
    ), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchPostings(offset + limit),
        disabled: loading || !result.hasMore,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "Next →"
    ))));
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
    const [copied, setCopied] = React.useState(null);
    const [selected, setSelected] = React.useState(null);
    const [lookupId, setLookupId] = React.useState("");
    const [usageOnly, setUsageOnly] = React.useState(false);
    const [usageTotals, setUsageTotals] = React.useState({});
    const [cursorStack, setCursorStack] = React.useState([]);
    const [hasMore, setHasMore] = React.useState(false);
    async function fetchCustomers(selectedMode, startingAfter, nextStack) {
      setLoading(true);
      setError(null);
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
        setUsageTotals({});
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
    React.useEffect(() => {
      if (!usageOnly || !(customers == null ? void 0 : customers.length)) return;
      let cancelled = false;
      async function run() {
        const headers = { ...sessionHeaders(), "X-Stripe-Mode": mode };
        for (const c of customers ?? []) {
          if (cancelled) return;
          try {
            let total = 0;
            const results = await Promise.all(meters.map((m) => fetch(
              `/element/stripe/api/stripe/meters/usage?event=${encodeURIComponent(m.eventName)}&customer=${encodeURIComponent(c.id)}${usageRangeParams()}`,
              { credentials: "include", headers }
            )));
            for (const res of results) {
              if (res.ok) total += sumUsage(await res.json());
            }
            if (!cancelled) setUsageTotals((prev) => ({ ...prev, [c.id]: total }));
          } catch {
            if (!cancelled) setUsageTotals((prev) => ({ ...prev, [c.id]: null }));
          }
        }
      }
      run();
      return () => {
        cancelled = true;
      };
    }, [usageOnly, customers, meters, mode]);
    const filteredCustomers = usageOnly ? (customers ?? []).filter((c) => (usageTotals[c.id] ?? 0) > 0) : customers ?? [];
    const usageFilterSettled = !usageOnly || filteredCustomers.length > 0 || (customers ?? []).every((c) => usageTotals[c.id] !== void 0);
    const livemodeMismatch = (customers ?? []).length > 0 && (mode === "production" ? customers.some((c) => !c.livemode) : customers.some((c) => c.livemode));
    function handleCopy(id) {
      navigator.clipboard.writeText(id).then(() => {
        setCopied(id);
        setTimeout(() => setCopied(null), 2e3);
      });
    }
    if (selected) {
      return /* @__PURE__ */ React.createElement(
        CustomerDetailView,
        {
          customerId: selected.id,
          fallbackName: displayName(selected),
          mode,
          onBack: () => setSelected(null)
        }
      );
    }
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-6xl" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between mb-1" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold" }, "Customers"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => fetchCustomers(mode, currentCursor, cursorStack),
        disabled: loading,
        className: "rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      loading ? "Refreshing…" : "Refresh"
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-4" }, "Every customer on this Stripe account in the selected mode — each one is billing-enabled there. Click a row for its full billing view: metered usage, charges, invoices, payment attempts, subscriptions, and its posting ledger."), /* @__PURE__ */ React.createElement("div", { className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "customers-mode", className: "block text-sm font-medium" }, "Mode"), /* @__PURE__ */ React.createElement(
      "select",
      {
        id: "customers-mode",
        value: mode,
        onChange: (e) => setMode(e.target.value),
        className: "rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
      },
      MODES.map((m) => /* @__PURE__ */ React.createElement("option", { key: m, value: m }, m === "production" ? "Production (live)" : "Sandbox (test)"))
    )), /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "customers-usage-only", className: "block text-sm font-medium" }, "Filter"), /* @__PURE__ */ React.createElement("label", { htmlFor: "customers-usage-only", className: "flex items-center gap-2 h-10 px-3 rounded-md border border-border bg-background text-sm cursor-pointer" }, /* @__PURE__ */ React.createElement(
      "input",
      {
        id: "customers-usage-only",
        type: "checkbox",
        checked: usageOnly,
        onChange: (e) => setUsageOnly(e.target.checked),
        className: "accent-primary"
      }
    ), usageOnly && Object.keys(usageTotals).length > 0 && usageFilterSettled ? `With usage only (${filteredCustomers.length})` : "With usage only")), /* @__PURE__ */ React.createElement(
      "form",
      {
        className: "space-y-1.5",
        onSubmit: (e) => {
          e.preventDefault();
          if (lookupId.trim()) setSelected({ id: lookupId.trim(), name: null, email: null, metadata: {}, created: null, livemode: mode === "production" });
        }
      },
      /* @__PURE__ */ React.createElement("label", { htmlFor: "customers-lookup", className: "block text-sm font-medium" }, "Open by ID"),
      /* @__PURE__ */ React.createElement(
        "input",
        {
          id: "customers-lookup",
          type: "text",
          value: lookupId,
          onChange: (e) => setLookupId(e.target.value),
          placeholder: "cus_… (any mode)",
          className: "w-64 rounded-md border border-border bg-background px-3 py-2 text-sm font-mono placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
        }
      )
    )), livemodeMismatch && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-yellow-500/50 bg-yellow-500/10 p-3 text-sm text-yellow-700 dark:text-yellow-500 mb-4" }, "The customer data is ", /* @__PURE__ */ React.createElement("strong", null, mode === "production" ? "test-mode" : "live-mode"), " but you are viewing the", " ", /* @__PURE__ */ React.createElement("strong", null, mode), " key — the ", mode, " API key configured for this deployment looks like a", " ", mode === "production" ? "test" : "live", " key. Check the Configuration tab."), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), customers && !loading && filteredCustomers.length === 0 && usageFilterSettled && /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, usageOnly ? "No customers with metered usage in the trailing window." : `No customers exist in ${mode} mode yet.`), filteredCustomers.length > 0 && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm table-fixed" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground w-[18rem]" }, "Customer ID"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Name"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Email"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Created"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Stripe"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2" }))), /* @__PURE__ */ React.createElement("tbody", null, filteredCustomers.map((c, i) => /* @__PURE__ */ React.createElement(
      "tr",
      {
        key: c.id,
        onClick: () => setSelected(c),
        className: `${i % 2 === 0 ? "" : "bg-muted/20"} cursor-pointer hover:bg-muted/40 transition-colors`
      },
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs", onClick: (e) => e.stopPropagation() }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-2" }, /* @__PURE__ */ React.createElement(
        "button",
        {
          onClick: () => handleCopy(c.id),
          title: "Copy customer ID",
          className: "truncate max-w-[12rem] text-left hover:underline"
        },
        c.id
      ), /* @__PURE__ */ React.createElement("span", { className: "w-14 shrink-0 text-[10px] text-green-600" }, copied === c.id ? "copied!" : ""))),
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 truncate" }, displayName(c)),
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs truncate" }, c.email ?? "—"),
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, c.created ? new Date(c.created).toLocaleDateString() : "—"),
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs whitespace-nowrap", onClick: (e) => e.stopPropagation() }, /* @__PURE__ */ React.createElement(
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
      )),
      /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-right text-xs text-muted-foreground" }, "Details →")
    ))))), /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-3 text-sm mt-4" }, /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: () => {
          const prevStack = cursorStack.slice(0, -1);
          fetchCustomers(mode, prevStack.length ? prevStack[prevStack.length - 1] : null, prevStack);
        },
        disabled: loading || cursorStack.length === 0,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "← Previous"
    ), /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground" }, cursorStack.length > 0 ? `Page ${cursorStack.length + 1}` : "Page 1"), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: customers && customers.length > 0 ? () => fetchCustomers(mode, customers[customers.length - 1].id, [...cursorStack, customers[customers.length - 1].id]) : void 0,
        disabled: loading || !hasMore,
        className: "rounded-md border border-border px-3 py-1.5 hover:bg-muted/40 disabled:opacity-50 transition-colors"
      },
      "Next →"
    ))));
  }
  function CustomerDetailView({ customerId, fallbackName, mode, onBack }) {
    const [meters, setMeters] = React.useState([]);
    const billing = useCustomerBilling(customerId, mode, meters);
    React.useEffect(() => {
      fetch("/element/stripe/api/stripe/meters?active=true&limit=100", {
        credentials: "include",
        headers: { ...sessionHeaders(), "X-Stripe-Mode": mode }
      }).then((res) => res.ok ? res.json() : []).then(setMeters).catch(() => setMeters([]));
    }, [mode]);
    const days = React.useMemo(() => {
      const list = [];
      for (let i = USAGE_DAYS - 1; i >= 0; i--) {
        const d = /* @__PURE__ */ new Date();
        d.setDate(d.getDate() - i);
        list.push({ key: d.toDateString(), label: d.toLocaleDateString(void 0, { month: "short", day: "numeric" }) });
      }
      return list;
    }, []);
    const hasUsage = Object.values(billing.usageByDay).some((days2) => Object.values(days2).some((v) => v !== 0));
    function formatUsage(n) {
      return n === 0 ? "·" : Number.isInteger(n) ? String(n) : n.toFixed(4);
    }
    function handlePortalLink() {
      fetch(`/element/stripe/api/stripe/customer/${encodeURIComponent(customerId)}/portal-session`, {
        method: "POST",
        credentials: "include",
        headers: { ...sessionHeaders(), "X-Stripe-Mode": mode }
      }).then(async (res) => {
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        window.open((await res.json()).url, "_blank");
      }).catch(() => {
      });
    }
    const onFile = billing.paymentMethods && billing.paymentMethods.length > 0 ? billing.paymentMethods.filter((m) => m.brand).map((m) => `${m.brand.toUpperCase()} ••${m.last4 ?? ""}`).join(", ") : null;
    const weekTotal = Object.values(billing.usageWeekTotals).reduce((s, v) => s + v, 0);
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-6xl" }, /* @__PURE__ */ React.createElement("button", { onClick: onBack, className: "text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors" }, "← All customers"), /* @__PURE__ */ React.createElement("div", { className: "flex items-center justify-between mb-1" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold" }, fallbackName), /* @__PURE__ */ React.createElement(
      "button",
      {
        onClick: handlePortalLink,
        className: "rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted/40 transition-colors"
      },
      "Portal Link"
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-mono text-muted-foreground mb-4" }, customerId, " · ", mode === "production" ? "production (live)" : "sandbox (test)", " ·", " ", /* @__PURE__ */ React.createElement("a", { href: stripeDashboardUrl(mode, `/customers/${customerId}`), target: "_blank", rel: "noreferrer", className: "text-primary hover:underline" }, "Stripe Dashboard ↗"), " · ", /* @__PURE__ */ React.createElement("a", { href: stripeDashboardUrl(mode, "/billing/meters"), target: "_blank", rel: "noreferrer", className: "text-primary hover:underline" }, "Meters ↗")), /* @__PURE__ */ React.createElement("div", { className: "grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6" }, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Payment method"), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, billing.loading || billing.paymentMethods === null ? "—" : onFile ?? "None on file")), /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Upcoming invoice"), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, billing.loading || billing.upcoming === null ? "—" : billing.upcoming === "none" ? "Nothing scheduled" : `${formatMoney(billing.upcoming.total, billing.upcoming.currency)} · due ${billing.upcoming.periodEnd ? new Date(billing.upcoming.periodEnd).toLocaleDateString() : "—"}`)), /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border p-3" }, /* @__PURE__ */ React.createElement("p", { className: "text-xs text-muted-foreground mb-1" }, "Metered usage, last ", USAGE_DAYS, " days"), /* @__PURE__ */ React.createElement("p", { className: "text-sm font-medium" }, billing.loading ? "—" : formatUsage(weekTotal)))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement("h2", { className: "text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2" }, "Metered usage by day (Stripe aggregates; local days)"), meters.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No active meters on this account.") : !hasUsage ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No usage recorded for this customer in the window.") : /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto max-w-2xl" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-xs" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40 text-muted-foreground" }, /* @__PURE__ */ React.createElement("th", { className: "px-3 py-2 text-left font-medium" }, "Day"), meters.map((m) => /* @__PURE__ */ React.createElement("th", { key: m.eventName, className: "px-3 py-2 text-right font-mono font-medium" }, m.eventName)), /* @__PURE__ */ React.createElement("th", { className: "px-3 py-2 text-right font-medium" }, "Total"))), /* @__PURE__ */ React.createElement("tbody", null, [...days].reverse().map(({ key, label }) => /* @__PURE__ */ React.createElement("tr", { key }, /* @__PURE__ */ React.createElement("td", { className: "px-3 py-1 text-muted-foreground" }, label), meters.map((m) => {
      var _a2;
      const value = ((_a2 = billing.usageByDay[m.eventName]) == null ? void 0 : _a2[key]) ?? 0;
      return /* @__PURE__ */ React.createElement("td", { key: m.eventName, className: `px-3 py-1 text-right font-mono ${value !== 0 ? "font-medium text-foreground" : "text-muted-foreground/50"}` }, billing.usage[m.eventName] === "error" ? /* @__PURE__ */ React.createElement("span", { className: "text-destructive" }, "err") : formatUsage(value));
    }), /* @__PURE__ */ React.createElement("td", { className: "px-3 py-1 text-right font-mono text-muted-foreground" }, (() => {
      const dayTotal = meters.reduce((sum, m) => {
        var _a2;
        return sum + (((_a2 = billing.usageByDay[m.eventName]) == null ? void 0 : _a2[key]) ?? 0);
      }, 0);
      return dayTotal === 0 ? "·" : formatUsage(dayTotal);
    })()))))))), /* @__PURE__ */ React.createElement(CustomerBillingTables, { billing }), /* @__PURE__ */ React.createElement("div", { className: "mt-6" }, /* @__PURE__ */ React.createElement(CustomerPostingsSection, { customerId })));
  }
  function CustomerBillingTables({ billing }) {
    var _a2, _b, _c, _d;
    return /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Charges (one-time purchases)"), /* @__PURE__ */ React.createElement(DataTable, { empty: billing.charges !== null && billing.charges.length === 0 ? "No charges." : null, failed: billing.charges === null }, (_a2 = billing.charges) == null ? void 0 : _a2.map((ch, i) => /* @__PURE__ */ React.createElement("tr", { key: ch.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, ch.created ? new Date(ch.created).toLocaleDateString() : "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(ch.amount, ch.currency), ch.refundedAmount > 0 && !ch.refunded && /* @__PURE__ */ React.createElement("span", { className: "text-xs text-orange-600 ml-1" }, "(−", formatMoney(ch.refundedAmount, ch.currency), ")")), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: ch.refunded ? "refunded" : ch.status, styles: { succeeded: "bg-green-500/10 text-green-700 dark:text-green-400", failed: "bg-destructive/10 text-destructive", refunded: "bg-orange-500/10 text-orange-600" } })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs max-w-xs truncate" }, ch.description ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, ch.id), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, ch.receiptUrl ? /* @__PURE__ */ React.createElement("a", { href: ch.receiptUrl, target: "_blank", rel: "noreferrer", className: "text-primary hover:underline" }, "Receipt ↗") : "—"))))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Invoices"), /* @__PURE__ */ React.createElement(DataTable, { empty: billing.invoices !== null && billing.invoices.length === 0 ? "No invoices." : null, failed: billing.invoices === null }, (_b = billing.invoices) == null ? void 0 : _b.map((inv, i) => /* @__PURE__ */ React.createElement("tr", { key: inv.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, new Date(inv.created).toLocaleDateString()), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(inv.amountPaid, inv.currency)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: inv.status, styles: {} })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, inv.id))))), /* @__PURE__ */ React.createElement("div", { className: "mb-6" }, /* @__PURE__ */ React.createElement(SectionHeading, null, "Payment attempts"), /* @__PURE__ */ React.createElement(DataTable, { empty: billing.intents !== null && billing.intents.length === 0 ? "No PaymentIntents." : null, failed: billing.intents === null }, (_c = billing.intents) == null ? void 0 : _c.map((pi, i) => /* @__PURE__ */ React.createElement("tr", { key: pi.id, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, pi.created ? new Date(pi.created).toLocaleDateString() : "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 whitespace-nowrap" }, formatMoney(pi.amount, pi.currency)), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: pi.status, styles: { succeeded: "bg-green-500/10 text-green-700 dark:text-green-400", canceled: "bg-muted text-muted-foreground" } })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, pi.id))))), /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement(SectionHeading, null, "Subscriptions"), /* @__PURE__ */ React.createElement(DataTable, { empty: billing.subscriptions !== null && billing.subscriptions.length === 0 ? "No subscriptions." : null, failed: billing.subscriptions === null }, (_d = billing.subscriptions) == null ? void 0 : _d.map((s, i) => /* @__PURE__ */ React.createElement("tr", { key: s.subscriptionId, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, s.subscriptionId), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement(Badge, { value: s.status, styles: { active: "bg-green-500/10 text-green-700 dark:text-green-400", trialing: "bg-blue-500/10 text-blue-700 dark:text-blue-400", past_due: "bg-yellow-500/10 text-yellow-700 dark:text-yellow-500", canceled: "bg-muted text-muted-foreground" } })), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs" }, s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : "—"))))));
  }
  function SectionHeading({ children }) {
    return /* @__PURE__ */ React.createElement("h2", { className: "text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-2" }, children);
  }
  function Badge({ value, styles }) {
    return /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${styles[value ?? ""] ?? "bg-muted text-muted-foreground"}` }, value ?? "—");
  }
  function DataTable({ children, empty, failed, headers }) {
    if (failed) return /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "Failed to load.");
    if (empty) return /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, empty);
    return /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, headers && /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, headers.map((h) => /* @__PURE__ */ React.createElement("th", { key: h, className: "px-4 py-2 text-left font-medium text-muted-foreground" }, h)))), /* @__PURE__ */ React.createElement("tbody", null, children)));
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
    const [copied, setCopied] = React.useState(null);
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
    )), /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground mb-6" }, "The global posting ledger: what each billing meter-event posting attempt actually did, newest first. Postings and failures made via recordMeterEvent are recorded automatically by this element; skips (exempt / no customer) are recorded by consumer elements at their reconciliation decision points. Click a customer ID to copy it, or open the customer in the Customers tab for its full billing view."), /* @__PURE__ */ React.createElement("div", { className: "flex flex-wrap gap-3 mb-4 items-end" }, /* @__PURE__ */ React.createElement("div", { className: "space-y-1.5" }, /* @__PURE__ */ React.createElement("label", { className: "block text-sm font-medium" }, "Mode"), /* @__PURE__ */ React.createElement(
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
    ))), error && /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive mb-4" }, error), result && (result.records.length === 0 ? /* @__PURE__ */ React.createElement("p", { className: "text-sm text-muted-foreground" }, "No posting records found. The ledger fills as meter events are posted (recorded automatically) and as consumer elements record their skip decisions — if nothing has posted since this feature deployed and the consumer wiring isn't in place yet, it will be empty.") : /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement("div", { className: "rounded-md border border-border overflow-x-auto mb-4" }, /* @__PURE__ */ React.createElement("table", { className: "w-full text-sm" }, /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", { className: "border-b border-border bg-muted/40" }, /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Time"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Outcome"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Mode"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Identifier"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Customer"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Event"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-right font-medium text-muted-foreground" }, "Qty"), /* @__PURE__ */ React.createElement("th", { className: "px-4 py-2 text-left font-medium text-muted-foreground" }, "Detail"))), /* @__PURE__ */ React.createElement("tbody", null, result.records.map((rec, i) => {
      var _a2;
      return /* @__PURE__ */ React.createElement("tr", { key: `${rec.identifier}-${rec.at}-${i}`, className: i % 2 === 0 ? "" : "bg-muted/20" }, /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-muted-foreground text-xs whitespace-nowrap" }, new Date(rec.at).toLocaleString()), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2" }, /* @__PURE__ */ React.createElement("span", { className: `inline-block rounded px-2 py-0.5 text-xs font-medium ${OUTCOME_STYLES[rec.outcome ?? ""] ?? "bg-muted text-muted-foreground"}` }, rec.outcome ?? "?")), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, ((_a2 = rec.mode) == null ? void 0 : _a2.toLowerCase()) ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, rec.identifier), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 font-mono text-xs" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-2" }, /* @__PURE__ */ React.createElement(
        "button",
        {
          onClick: () => {
            if (!rec.customerId) return;
            navigator.clipboard.writeText(rec.customerId).then(() => {
              setCopied(rec.customerId);
              setTimeout(() => setCopied(null), 2e3);
            });
          },
          title: rec.customerId ? "Copy customer ID" : void 0,
          className: "truncate max-w-[10rem] text-left hover:underline"
        },
        rec.customerId ?? "—"
      ), /* @__PURE__ */ React.createElement("span", { className: "w-14 shrink-0 text-[10px] text-green-600" }, copied && rec.customerId === copied ? "copied!" : ""))), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs" }, rec.eventName ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-right" }, rec.quantity ?? "—"), /* @__PURE__ */ React.createElement("td", { className: "px-4 py-2 text-xs text-muted-foreground max-w-xs truncate", title: rec.detail ?? "" }, rec.detail ?? ""));
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
    )))), activeTab === "config" && /* @__PURE__ */ React.createElement(StripeConfigPlugin, null), activeTab === "customers" && /* @__PURE__ */ React.createElement(StripeCustomersPlugin, null), activeTab === "postings" && /* @__PURE__ */ React.createElement(StripePostingPlugin, null), activeTab === "events" && /* @__PURE__ */ React.createElement(StripeEventLogPlugin, null));
  }
  (_a = window.__elementsPlugins) == null ? void 0 : _a.register("stripe", StripePlugin);
})(window.React);
