import { useEffect, useState } from "react";

const API_URL = import.meta.env.VITE_API_URL;

function Dashboard({ onLogout }) {
    const [issues, setIssues] = useState([]);
    const [workloads, setWorkloads] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // NAVIGATION
    const [activeView, setActiveView] = useState("overview");

    // INCIDENT FILTERS
    const [incidentSearch, setIncidentSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [priorityFilter, setPriorityFilter] = useState("ALL");

    // CREATE INCIDENT
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [creating, setCreating] = useState(false);
    const [createError, setCreateError] = useState("");

    const [newIssue, setNewIssue] = useState({
        title: "",
        description: "",
        impact: 3,
        urgency: 3,
        affectedUsers: 0,
        deadline: "",
        estimatedHours: 1,
    });

    // INCIDENT DETAILS
    const [selectedIssue, setSelectedIssue] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const [actionError, setActionError] = useState("");
    const [actionSuccess, setActionSuccess] = useState("");

    // =========================================================
    // AUTH
    // =========================================================

    const getToken = () => {
        return (
            localStorage.getItem("pulsedesk_token") ||
            sessionStorage.getItem("pulsedesk_token")
        );
    };

    const getHeaders = () => {
        const token = getToken();

        return {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        };
    };

    // =========================================================
    // LOAD DASHBOARD
    // =========================================================

    const loadDashboard = async (showLoadingScreen = false) => {
        const token = getToken();

        if (!token) {
            onLogout();
            return;
        }

        try {
            if (showLoadingScreen) {
                setLoading(true);
            }

            setError("");

            const headers = getHeaders();

            const [issuesResponse, workloadsResponse] =
                await Promise.all([
                    fetch(`${API_URL}/api/issues`, {
                        headers,
                    }),
                    fetch(`${API_URL}/api/workloads`, {
                        headers,
                    }),
                ]);

            if (
                issuesResponse.status === 401 ||
                issuesResponse.status === 403 ||
                workloadsResponse.status === 401 ||
                workloadsResponse.status === 403
            ) {
                onLogout();
                return;
            }

            if (!issuesResponse.ok) {
                throw new Error("Unable to load incidents.");
            }

            if (!workloadsResponse.ok) {
                throw new Error("Unable to load team workload.");
            }

            const issuesData = await issuesResponse.json();
            const workloadsData = await workloadsResponse.json();

            setIssues(
                Array.isArray(issuesData)
                    ? issuesData
                    : []
            );

            setWorkloads(
                Array.isArray(workloadsData)
                    ? workloadsData
                    : []
            );
        } catch (err) {
            console.error("Dashboard error:", err);

            setError(
                err.message ||
                "Unable to load dashboard."
            );
        } finally {
            if (showLoadingScreen) {
                setLoading(false);
            }
        }
    };

    useEffect(() => {
        loadDashboard(true);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // =========================================================
    // CREATE INCIDENT
    // =========================================================

    const handleIssueChange = (event) => {
        const { name, value } = event.target;

        setNewIssue((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleCreateIssue = async (event) => {
        event.preventDefault();

        setCreateError("");

        if (!newIssue.title.trim()) {
            setCreateError("Title is required.");
            return;
        }

        if (!newIssue.description.trim()) {
            setCreateError("Description is required.");
            return;
        }

        const token = getToken();

        if (!token) {
            onLogout();
            return;
        }

        try {
            setCreating(true);

            const requestBody = {
                title: newIssue.title.trim(),
                description: newIssue.description.trim(),
                impact: Number(newIssue.impact),
                urgency: Number(newIssue.urgency),
                affectedUsers: Number(newIssue.affectedUsers),
                estimatedHours: Number(newIssue.estimatedHours),
            };

            if (newIssue.deadline) {
                requestBody.deadline = newIssue.deadline;
            }

            const response = await fetch(
                `${API_URL}/api/issues`,
                {
                    method: "POST",
                    headers: getHeaders(),
                    body: JSON.stringify(requestBody),
                }
            );

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                onLogout();
                return;
            }

            let data = {};

            try {
                data = await response.json();
            } catch {
                // Empty response.
            }

            if (!response.ok) {
                throw new Error(
                    data.message ||
                    data.error ||
                    "Unable to create incident."
                );
            }

            setIssues((previousIssues) => [
                data,
                ...previousIssues,
            ]);

            setNewIssue({
                title: "",
                description: "",
                impact: 3,
                urgency: 3,
                affectedUsers: 0,
                deadline: "",
                estimatedHours: 1,
            });

            setShowCreateModal(false);
        } catch (err) {
            console.error("Create incident error:", err);

            setCreateError(
                err.message ||
                "Unable to create incident."
            );
        } finally {
            setCreating(false);
        }
    };

    // =========================================================
    // INCIDENT DETAILS
    // =========================================================

    const openIncidentDetails = async (issueId) => {
        const token = getToken();

        if (!token) {
            onLogout();
            return;
        }

        try {
            setDetailsLoading(true);
            setActionError("");
            setActionSuccess("");

            const response = await fetch(
                `${API_URL}/api/issues/${issueId}`,
                {
                    headers: getHeaders(),
                }
            );

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                onLogout();
                return;
            }

            if (!response.ok) {
                throw new Error(
                    "Unable to load incident details."
                );
            }

            const data = await response.json();
            setSelectedIssue(data);
        } catch (err) {
            console.error("Incident details error:", err);

            setError(
                err.message ||
                "Unable to load incident details."
            );
        } finally {
            setDetailsLoading(false);
        }
    };

    const closeIncidentDetails = () => {
        if (actionLoading) {
            return;
        }

        setSelectedIssue(null);
        setActionError("");
        setActionSuccess("");
    };

    // =========================================================
    // AUTO ASSIGN
    // =========================================================

    const handleAutoAssign = async () => {
        if (!selectedIssue) {
            return;
        }

        try {
            setActionLoading(true);
            setActionError("");
            setActionSuccess("");

            const response = await fetch(
                `${API_URL}/api/issues/${selectedIssue.id}/auto-assign`,
                {
                    method: "POST",
                    headers: getHeaders(),
                }
            );

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                onLogout();
                return;
            }

            let data = {};

            try {
                data = await response.json();
            } catch {
                // Empty response.
            }

            if (!response.ok) {
                throw new Error(
                    data.message ||
                    data.error ||
                    "Unable to auto assign incident."
                );
            }

            setActionSuccess(
                data.message ||
                `Assigned to ${
                    data.assignedUserName ||
                    "team member"
                }.`
            );

            const issueResponse = await fetch(
                `${API_URL}/api/issues/${selectedIssue.id}`,
                {
                    headers: getHeaders(),
                }
            );

            if (issueResponse.ok) {
                const updatedIssue =
                    await issueResponse.json();

                setSelectedIssue(updatedIssue);
            }

            await loadDashboard(false);
        } catch (err) {
            console.error("Auto assign error:", err);

            setActionError(
                err.message ||
                "Unable to auto assign incident."
            );
        } finally {
            setActionLoading(false);
        }
    };

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    const handleStatusChange = async (newStatus) => {
        if (!selectedIssue) {
            return;
        }

        try {
            setActionLoading(true);
            setActionError("");
            setActionSuccess("");

            const response = await fetch(
                `${API_URL}/api/issues/${selectedIssue.id}/status`,
                {
                    method: "PATCH",
                    headers: getHeaders(),
                    body: JSON.stringify({
                        status: newStatus,
                    }),
                }
            );

            if (
                response.status === 401 ||
                response.status === 403
            ) {
                onLogout();
                return;
            }

            let data = {};

            try {
                data = await response.json();
            } catch {
                // Empty response.
            }

            if (!response.ok) {
                throw new Error(
                    data.message ||
                    data.error ||
                    "Unable to update incident status."
                );
            }

            setSelectedIssue(data);

            if (newStatus === "IN_PROGRESS") {
                setActionSuccess(
                    "Incident moved to In Progress."
                );
            }

            if (newStatus === "RESOLVED") {
                setActionSuccess(
                    "Incident resolved successfully."
                );
            }

            await loadDashboard(false);
        } catch (err) {
            console.error("Status update error:", err);

            setActionError(
                err.message ||
                "Unable to update incident status."
            );
        } finally {
            setActionLoading(false);
        }
    };

    // =========================================================
    // CALCULATIONS
    // =========================================================

    const totalIssues = issues.length;

    const criticalIssues = issues.filter(
        (issue) => issue.priorityLevel === "CRITICAL"
    ).length;

    const highIssues = issues.filter(
        (issue) => issue.priorityLevel === "HIGH"
    ).length;

    const openIssues = issues.filter(
        (issue) => issue.status === "OPEN"
    ).length;

    const resolvedIssues = issues.filter(
        (issue) => issue.status === "RESOLVED"
    ).length;

    const overloadedUsers = workloads.filter(
        (user) => user.workloadStatus === "OVERLOADED"
    ).length;

    // =========================================================
    // FILTER INCIDENTS
    // =========================================================

    const filteredIssues = [...issues]
        .sort((a, b) => b.id - a.id)
        .filter((issue) => {
            const search =
                incidentSearch.trim().toLowerCase();

            const title =
                issue.title?.toLowerCase() || "";

            const description =
                issue.description?.toLowerCase() || "";

            const matchesSearch =
                !search ||
                title.includes(search) ||
                description.includes(search) ||
                String(issue.id).includes(search);

            const matchesStatus =
                statusFilter === "ALL" ||
                issue.status === statusFilter;

            const matchesPriority =
                priorityFilter === "ALL" ||
                issue.priorityLevel === priorityFilter;

            return (
                matchesSearch &&
                matchesStatus &&
                matchesPriority
            );
        });

    // =========================================================
    // HELPERS
    // =========================================================

    const getPriorityClass = (priority) => {
        switch (priority) {
            case "CRITICAL":
                return "priority-critical";
            case "HIGH":
                return "priority-high";
            case "MEDIUM":
                return "priority-medium";
            case "LOW":
                return "priority-low";
            default:
                return "priority-default";
        }
    };

    const getWorkloadClass = (status) => {
        switch (status) {
            case "AVAILABLE":
                return "workload-available";
            case "BUSY":
                return "workload-busy";
            case "OVERLOADED":
                return "workload-overloaded";
            default:
                return "";
        }
    };

    const formatStatus = (status) => {
        if (!status) {
            return "UNKNOWN";
        }

        return status.replaceAll("_", " ");
    };

    const formatDeadline = (deadline) => {
        if (!deadline) {
            return "No deadline";
        }

        const date = new Date(deadline);

        if (Number.isNaN(date.getTime())) {
            return deadline;
        }

        return date.toLocaleString();
    };

    // =========================================================
    // LOADING
    // =========================================================

    if (loading) {
        return (
            <div className="dashboard-loading">
                <div className="loading-pulse"></div>

                <h2>Loading PulseDesk</h2>

                <p>
                    Preparing your intelligent workspace...
                </p>
            </div>
        );
    }

    // =========================================================
    // MAIN
    // =========================================================

    return (
        <div className="dashboard-page">

            <div className="dashboard-glow dashboard-glow-one"></div>
            <div className="dashboard-glow dashboard-glow-two"></div>

            {/* SIDEBAR */}

            <aside className="dashboard-sidebar">

                <div className="dashboard-logo">
                    <span className="pulse-dot"></span>
                    PulseDesk
                </div>

                <nav className="dashboard-nav">

                    <button
                        type="button"
                        className={`nav-item ${
                            activeView === "overview"
                                ? "active"
                                : ""
                        }`}
                        onClick={() =>
                            setActiveView("overview")
                        }
                    >
                        <span>◈</span>
                        Overview
                    </button>

                    <button
                        type="button"
                        className={`nav-item ${
                            activeView === "incidents"
                                ? "active"
                                : ""
                        }`}
                        onClick={() =>
                            setActiveView("incidents")
                        }
                    >
                        <span>⚡</span>
                        Incidents
                    </button>

                </nav>

                <div className="sidebar-bottom">

                    <div className="sidebar-security">
                        <span className="security-dot"></span>

                        <div>
                            <strong>
                                Secure session
                            </strong>

                            <small>
                                JWT authenticated
                            </small>
                        </div>
                    </div>

                    <button
                        type="button"
                        className="logout-button"
                        onClick={onLogout}
                    >
                        <span>↪</span>
                        Logout
                    </button>

                </div>

            </aside>

            {/* MAIN CONTENT */}

            <main className="dashboard-main">

                <header className="dashboard-header">

                    <div>

                        <div className="dashboard-eyebrow">
                            INTELLIGENT INCIDENT MANAGEMENT
                        </div>

                        <h1>
                            {activeView === "overview"
                                ? "Command Center"
                                : "All Incidents"}
                        </h1>

                        <p>
                            {activeView === "overview"
                                ? "Monitor priorities, team capacity and incident progress in real time."
                                : "Search, filter and manage every incident from one place."}
                        </p>

                    </div>

                    <div className="header-actions">

                        <div className="live-status">
                            <span></span>
                            LIVE
                        </div>

                        <button
                            type="button"
                            className="create-issue-button"
                            onClick={() => {
                                setCreateError("");
                                setShowCreateModal(true);
                            }}
                        >
                            <span>＋</span>
                            Create Incident
                        </button>

                    </div>

                </header>

                {error && (
                    <div className="dashboard-error">
                        <span>⚠️</span>
                        {error}
                    </div>
                )}

                {/* OVERVIEW */}

                {activeView === "overview" && (
                    <>
                        <section className="stats-grid">

                            <article className="stat-card">

                                <div className="stat-top">
                                    <div className="stat-icon blue">
                                        ◈
                                    </div>

                                    <span className="stat-label">
                                        TOTAL
                                    </span>
                                </div>

                                <strong className="stat-number">
                                    {totalIssues}
                                </strong>

                                <p>Total incidents</p>

                            </article>

                            <article className="stat-card critical-card">

                                <div className="stat-top">
                                    <div className="stat-icon red">
                                        ⚡
                                    </div>

                                    <span className="stat-label">
                                        URGENT
                                    </span>
                                </div>

                                <strong className="stat-number">
                                    {criticalIssues}
                                </strong>

                                <p>Critical incidents</p>

                            </article>

                            <article className="stat-card">

                                <div className="stat-top">
                                    <div className="stat-icon orange">
                                        ◉
                                    </div>

                                    <span className="stat-label">
                                        ACTIVE
                                    </span>
                                </div>

                                <strong className="stat-number">
                                    {openIssues}
                                </strong>

                                <p>Open incidents</p>

                            </article>

                            <article className="stat-card">

                                <div className="stat-top">
                                    <div className="stat-icon green">
                                        ✓
                                    </div>

                                    <span className="stat-label">
                                        COMPLETE
                                    </span>
                                </div>

                                <strong className="stat-number">
                                    {resolvedIssues}
                                </strong>

                                <p>Resolved incidents</p>

                            </article>

                        </section>

                        <section className="dashboard-content-grid">

                            <div className="dashboard-panel incidents-panel">

                                <div className="panel-header">

                                    <div>
                                        <span className="panel-eyebrow">
                                            PRIORITY QUEUE
                                        </span>

                                        <h2>
                                            Recent Incidents
                                        </h2>
                                    </div>

                                    <div className="incident-summary">
                                        <span>
                                            {criticalIssues} Critical
                                        </span>

                                        <span>
                                            {highIssues} High
                                        </span>
                                    </div>

                                </div>

                                {issues.length === 0 ? (
                                    <div className="empty-state">
                                        <div className="empty-icon">
                                            ✓
                                        </div>

                                        <h3>All clear</h3>

                                        <p>
                                            No incidents have been
                                            created yet.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="incident-list">

                                        {[...issues]
                                            .sort(
                                                (a, b) =>
                                                    b.id - a.id
                                            )
                                            .slice(0, 6)
                                            .map((issue) => (
                                                <div
                                                    className="incident-row clickable-incident"
                                                    key={issue.id}
                                                    role="button"
                                                    tabIndex={0}
                                                    onClick={() =>
                                                        openIncidentDetails(
                                                            issue.id
                                                        )
                                                    }
                                                    onKeyDown={(event) => {
                                                        if (
                                                            event.key === "Enter" ||
                                                            event.key === " "
                                                        ) {
                                                            openIncidentDetails(
                                                                issue.id
                                                            );
                                                        }
                                                    }}
                                                >

                                                    <div className="incident-main">

                                                        <div
                                                            className={`priority-indicator ${getPriorityClass(
                                                                issue.priorityLevel
                                                            )}`}
                                                        ></div>

                                                        <div>
                                                            <strong>
                                                                {issue.title}
                                                            </strong>

                                                            <small>
                                                                #{issue.id}
                                                                {" • "}
                                                                {formatStatus(
                                                                    issue.status
                                                                )}
                                                            </small>
                                                        </div>

                                                    </div>

                                                    <div className="incident-meta">

                                                        <span
                                                            className={`priority-badge ${getPriorityClass(
                                                                issue.priorityLevel
                                                            )}`}
                                                        >
                                                            {issue.priorityLevel ||
                                                                "UNRANKED"}
                                                        </span>

                                                        <div className="priority-score">
                                                            <small>
                                                                SCORE
                                                            </small>

                                                            <strong>
                                                                {issue.priorityScore ??
                                                                    "—"}
                                                            </strong>
                                                        </div>

                                                    </div>

                                                </div>
                                            ))}

                                    </div>
                                )}

                            </div>

                            {/* PART 1 ENDS HERE */}
                            {/* WORKLOAD */}

                            <div className="dashboard-panel workload-panel">

                                <div className="panel-header">

                                    <div>
                                        <span className="panel-eyebrow">
                                            TEAM CAPACITY
                                        </span>

                                        <h2>Workload</h2>
                                    </div>

                                    <div className="team-count">
                                        {workloads.length}

                                        <span>MEMBERS</span>
                                    </div>

                                </div>

                                {overloadedUsers > 0 && (
                                    <div className="capacity-warning">
                                        ⚠️ {overloadedUsers} team member
                                        {overloadedUsers > 1
                                            ? "s are"
                                            : " is"}{" "}
                                        overloaded
                                    </div>
                                )}

                                <div className="workload-list">

                                    {workloads.length === 0 ? (
                                        <div className="empty-state small">
                                            <p>
                                                No workload information available.
                                            </p>
                                        </div>
                                    ) : (
                                        workloads.map((user) => {
                                            const percentage = Math.min(
                                                user.workloadPercentage || 0,
                                                100
                                            );

                                            return (
                                                <div
                                                    className="workload-user"
                                                    key={user.userId}
                                                >

                                                    <div className="workload-user-header">

                                                        <div className="user-info">

                                                            <div className="user-avatar">
                                                                {user.name
                                                                    ? user.name
                                                                        .charAt(0)
                                                                        .toUpperCase()
                                                                    : "U"}
                                                            </div>

                                                            <div>
                                                                <strong>
                                                                    {user.name || "User"}
                                                                </strong>

                                                                <small
                                                                    className={getWorkloadClass(
                                                                        user.workloadStatus
                                                                    )}
                                                                >
                                                                    {user.workloadStatus ||
                                                                        "AVAILABLE"}
                                                                </small>
                                                            </div>

                                                        </div>

                                                        <div className="workload-hours">
                                                            <strong>
                                                                {user.workloadHours || 0}
                                                            </strong>

                                                            <span>
                                                                /
                                                                {user.maxCapacityHours ||
                                                                    40}
                                                                h
                                                            </span>
                                                        </div>

                                                    </div>

                                                    <div className="workload-track">
                                                        <div
                                                            className={`workload-fill ${getWorkloadClass(
                                                                user.workloadStatus
                                                            )}`}
                                                            style={{
                                                                width: `${percentage}%`,
                                                            }}
                                                        ></div>
                                                    </div>

                                                    <div className="workload-percentage">
                                                        {user.workloadPercentage || 0}%
                                                        capacity used
                                                    </div>

                                                </div>
                                            );
                                        })
                                    )}

                                </div>

                            </div>

                        </section>

                        {/* BOTTOM BAR */}

                        <section className="dashboard-bottom-bar">

                            <div>
                                <span className="bottom-icon">⚡</span>

                                <div>
                                    <strong>
                                        Smart Priority Engine
                                    </strong>

                                    <small>
                                        Automatically ranking incoming incidents
                                    </small>
                                </div>
                            </div>

                            <div>
                                <span className="bottom-icon">◎</span>

                                <div>
                                    <strong>
                                        Auto Assignment
                                    </strong>

                                    <small>
                                        Balancing workload across your team
                                    </small>
                                </div>
                            </div>

                            <div className="system-operational">
                                <span></span>
                                ALL SYSTEMS OPERATIONAL
                            </div>

                        </section>
                    </>
                )}

                {/* =================================================
                    ALL INCIDENTS
                ================================================= */}

                {activeView === "incidents" && (
                    <section className="dashboard-panel all-incidents-panel">

                        <div className="panel-header">

                            <div>
                                <span className="panel-eyebrow">
                                    INCIDENT MANAGEMENT
                                </span>

                                <h2>All Incidents</h2>

                                <p>
                                    Showing {filteredIssues.length} of{" "}
                                    {totalIssues} incidents
                                </p>
                            </div>

                        </div>

                        {/* FILTERS */}

                        <div className="incident-filters">

                            <div className="incident-search">

                                <span>⌕</span>

                                <input
                                    type="text"
                                    placeholder="Search by title, description or ID..."
                                    value={incidentSearch}
                                    onChange={(event) =>
                                        setIncidentSearch(
                                            event.target.value
                                        )
                                    }
                                />

                            </div>

                            <select
                                value={statusFilter}
                                onChange={(event) =>
                                    setStatusFilter(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="ALL">
                                    All Statuses
                                </option>

                                <option value="OPEN">
                                    Open
                                </option>

                                <option value="IN_PROGRESS">
                                    In Progress
                                </option>

                                <option value="RESOLVED">
                                    Resolved
                                </option>

                                <option value="CLOSED">
                                    Closed
                                </option>
                            </select>

                            <select
                                value={priorityFilter}
                                onChange={(event) =>
                                    setPriorityFilter(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="ALL">
                                    All Priorities
                                </option>

                                <option value="CRITICAL">
                                    Critical
                                </option>

                                <option value="HIGH">
                                    High
                                </option>

                                <option value="MEDIUM">
                                    Medium
                                </option>

                                <option value="LOW">
                                    Low
                                </option>
                            </select>

                            {(incidentSearch ||
                                statusFilter !== "ALL" ||
                                priorityFilter !== "ALL") && (
                                <button
                                    type="button"
                                    className="clear-filter-button"
                                    onClick={() => {
                                        setIncidentSearch("");
                                        setStatusFilter("ALL");
                                        setPriorityFilter("ALL");
                                    }}
                                >
                                    Clear Filters
                                </button>
                            )}

                        </div>

                        {/* INCIDENT RESULTS */}

                        {filteredIssues.length === 0 ? (
                            <div className="empty-state">

                                <div className="empty-icon">
                                    ⌕
                                </div>

                                <h3>No incidents found</h3>

                                <p>
                                    Try changing your search or filters.
                                </p>

                            </div>
                        ) : (
                            <div className="all-incidents-list">

                                {filteredIssues.map((issue) => (
                                    <div
                                        className="incident-row all-incident-row clickable-incident"
                                        key={issue.id}
                                        role="button"
                                        tabIndex={0}
                                        onClick={() =>
                                            openIncidentDetails(
                                                issue.id
                                            )
                                        }
                                        onKeyDown={(event) => {
                                            if (
                                                event.key === "Enter" ||
                                                event.key === " "
                                            ) {
                                                openIncidentDetails(
                                                    issue.id
                                                );
                                            }
                                        }}
                                    >

                                        <div className="incident-main">

                                            <div
                                                className={`priority-indicator ${getPriorityClass(
                                                    issue.priorityLevel
                                                )}`}
                                            ></div>

                                            <div>
                                                <strong>
                                                    {issue.title}
                                                </strong>

                                                <small>
                                                    #{issue.id}
                                                    {" • "}
                                                    {issue.assignedToName
                                                        ? `Assigned to ${issue.assignedToName}`
                                                        : "Unassigned"}
                                                </small>
                                            </div>

                                        </div>

                                        <div className="all-incident-info">

                                            <div className="all-incident-status">
                                                <small>
                                                    STATUS
                                                </small>

                                                <strong>
                                                    {formatStatus(
                                                        issue.status
                                                    )}
                                                </strong>
                                            </div>

                                            <span
                                                className={`priority-badge ${getPriorityClass(
                                                    issue.priorityLevel
                                                )}`}
                                            >
                                                {issue.priorityLevel ||
                                                    "UNRANKED"}
                                            </span>

                                            <div className="priority-score">
                                                <small>
                                                    SCORE
                                                </small>

                                                <strong>
                                                    {issue.priorityScore ??
                                                        "—"}
                                                </strong>
                                            </div>

                                        </div>

                                    </div>
                                ))}

                            </div>
                        )}

                    </section>
                )}

            </main>

            {/* =================================================
                CREATE INCIDENT MODAL
            ================================================= */}

            {showCreateModal && (
                <div className="incident-modal-overlay">

                    <div className="incident-modal">

                        <div className="incident-modal-header">

                            <div>
                                <span className="panel-eyebrow">
                                    NEW INCIDENT
                                </span>

                                <h2>Create Incident</h2>

                                <p>
                                    PulseDesk will automatically calculate
                                    its priority.
                                </p>
                            </div>

                            <button
                                type="button"
                                className="modal-close-button"
                                onClick={() =>
                                    setShowCreateModal(false)
                                }
                                disabled={creating}
                            >
                                ×
                            </button>

                        </div>

                        {createError && (
                            <div className="dashboard-error">
                                <span>⚠️</span>
                                {createError}
                            </div>
                        )}

                        <form
                            className="incident-form"
                            onSubmit={handleCreateIssue}
                        >

                            <div className="incident-form-field full-width">

                                <label>
                                    Incident title
                                </label>

                                <input
                                    type="text"
                                    name="title"
                                    value={newIssue.title}
                                    onChange={handleIssueChange}
                                    placeholder="Example: Payment service unavailable"
                                    disabled={creating}
                                    required
                                />

                            </div>

                            <div className="incident-form-field full-width">

                                <label>
                                    Description
                                </label>

                                <textarea
                                    name="description"
                                    value={newIssue.description}
                                    onChange={handleIssueChange}
                                    placeholder="Describe what is happening..."
                                    rows="4"
                                    disabled={creating}
                                    required
                                ></textarea>

                            </div>

                            <div className="incident-form-grid">

                                <div className="incident-form-field">

                                    <label>Impact</label>

                                    <select
                                        name="impact"
                                        value={newIssue.impact}
                                        onChange={handleIssueChange}
                                        disabled={creating}
                                    >
                                        <option value="1">
                                            1 - Very Low
                                        </option>

                                        <option value="2">
                                            2 - Low
                                        </option>

                                        <option value="3">
                                            3 - Medium
                                        </option>

                                        <option value="4">
                                            4 - High
                                        </option>

                                        <option value="5">
                                            5 - Critical
                                        </option>
                                    </select>

                                </div>

                                <div className="incident-form-field">

                                    <label>Urgency</label>

                                    <select
                                        name="urgency"
                                        value={newIssue.urgency}
                                        onChange={handleIssueChange}
                                        disabled={creating}
                                    >
                                        <option value="1">
                                            1 - Very Low
                                        </option>

                                        <option value="2">
                                            2 - Low
                                        </option>

                                        <option value="3">
                                            3 - Medium
                                        </option>

                                        <option value="4">
                                            4 - High
                                        </option>

                                        <option value="5">
                                            5 - Critical
                                        </option>
                                    </select>

                                </div>

                                <div className="incident-form-field">

                                    <label>
                                        Affected Users
                                    </label>

                                    <input
                                        type="number"
                                        name="affectedUsers"
                                        min="0"
                                        value={newIssue.affectedUsers}
                                        onChange={handleIssueChange}
                                        disabled={creating}
                                    />

                                </div>

                                <div className="incident-form-field">

                                    <label>
                                        Estimated Hours
                                    </label>

                                    <input
                                        type="number"
                                        name="estimatedHours"
                                        min="1"
                                        value={newIssue.estimatedHours}
                                        onChange={handleIssueChange}
                                        disabled={creating}
                                    />

                                </div>

                                <div className="incident-form-field full-width">

                                    <label>
                                        Deadline
                                    </label>

                                    <input
                                        type="datetime-local"
                                        name="deadline"
                                        value={newIssue.deadline}
                                        onChange={handleIssueChange}
                                        disabled={creating}
                                    />

                                </div>

                            </div>

                            <div className="incident-modal-actions">

                                <button
                                    type="button"
                                    className="modal-cancel-button"
                                    onClick={() =>
                                        setShowCreateModal(false)
                                    }
                                    disabled={creating}
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    className="create-issue-button"
                                    disabled={creating}
                                >
                                    {creating
                                        ? "Creating..."
                                        : "Create Incident"}
                                </button>

                            </div>

                        </form>

                    </div>

                </div>
            )}

            {/* =================================================
                DETAILS LOADING
            ================================================= */}

            {detailsLoading && !selectedIssue && (
                <div className="incident-modal-overlay">

                    <div className="incident-details-loading">

                        <div className="loading-pulse"></div>

                        <strong>
                            Loading incident...
                        </strong>

                    </div>

                </div>
            )}

            {/* =================================================
                INCIDENT DETAILS
            ================================================= */}

            {selectedIssue && (
                <div className="incident-modal-overlay">

                    <div className="incident-modal incident-details-modal">

                        <div className="incident-modal-header">

                            <div>

                                <span className="panel-eyebrow">
                                    INCIDENT #{selectedIssue.id}
                                </span>

                                <h2>
                                    {selectedIssue.title}
                                </h2>

                                <p>
                                    {selectedIssue.description ||
                                        "No description provided."}
                                </p>

                            </div>

                            <button
                                type="button"
                                className="modal-close-button"
                                onClick={closeIncidentDetails}
                                disabled={actionLoading}
                            >
                                ×
                            </button>

                        </div>

                        {/* PRIORITY */}

                        <div className="incident-priority-hero">

                            <div>
                                <small>PRIORITY</small>

                                <span
                                    className={`priority-badge ${getPriorityClass(
                                        selectedIssue.priorityLevel
                                    )}`}
                                >
                                    {selectedIssue.priorityLevel ||
                                        "UNRANKED"}
                                </span>
                            </div>

                            <div>
                                <small>
                                    PRIORITY SCORE
                                </small>

                                <strong>
                                    {selectedIssue.priorityScore ??
                                        "—"}
                                </strong>
                            </div>

                            <div>
                                <small>STATUS</small>

                                <strong>
                                    {formatStatus(
                                        selectedIssue.status
                                    )}
                                </strong>
                            </div>

                        </div>

                        {/* DETAILS */}

                        <div className="incident-details-grid">

                            <div className="incident-detail-card">
                                <small>IMPACT</small>

                                <strong>
                                    {selectedIssue.impact ?? "—"}/5
                                </strong>
                            </div>

                            <div className="incident-detail-card">
                                <small>URGENCY</small>

                                <strong>
                                    {selectedIssue.urgency ?? "—"}/5
                                </strong>
                            </div>

                            <div className="incident-detail-card">
                                <small>
                                    AFFECTED USERS
                                </small>

                                <strong>
                                    {selectedIssue.affectedUsers ?? 0}
                                </strong>
                            </div>

                            <div className="incident-detail-card">
                                <small>
                                    ESTIMATED WORK
                                </small>

                                <strong>
                                    {selectedIssue.estimatedHours ?? 0}h
                                </strong>
                            </div>

                        </div>

                        {/* ASSIGNMENT */}

                        <div className="incident-assignment-section">

                            <div>
                                <small>
                                    ASSIGNED TO
                                </small>

                                <strong>
                                    {selectedIssue.assignedToName ||
                                        "Unassigned"}
                                </strong>
                            </div>

                            <div>
                                <small>
                                    CREATED BY
                                </small>

                                <strong>
                                    {selectedIssue.createdByName ||
                                        "Unknown"}
                                </strong>
                            </div>

                        </div>

                        {/* DEADLINE */}

                        <div className="incident-deadline">

                            <small>DEADLINE</small>

                            <strong>
                                {formatDeadline(
                                    selectedIssue.deadline
                                )}
                            </strong>

                        </div>

                        {actionError && (
                            <div className="dashboard-error">
                                <span>⚠️</span>
                                {actionError}
                            </div>
                        )}

                        {actionSuccess && (
                            <div className="incident-action-success">
                                <span>✓</span>
                                {actionSuccess}
                            </div>
                        )}

                        {/* ACTION BUTTONS */}

                        <div className="incident-action-buttons">

                            {!selectedIssue.assignedToId &&
                                selectedIssue.status !== "RESOLVED" &&
                                selectedIssue.status !== "CLOSED" && (
                                    <button
                                        type="button"
                                        className="incident-auto-assign-button"
                                        onClick={handleAutoAssign}
                                        disabled={actionLoading}
                                    >
                                        ◎{" "}
                                        {actionLoading
                                            ? "Working..."
                                            : "Auto Assign"}
                                    </button>
                                )}

                            {selectedIssue.status === "OPEN" && (
                                <button
                                    type="button"
                                    className="incident-progress-button"
                                    onClick={() =>
                                        handleStatusChange(
                                            "IN_PROGRESS"
                                        )
                                    }
                                    disabled={actionLoading}
                                >
                                    ▶ Start Progress
                                </button>
                            )}

                            {selectedIssue.status !== "RESOLVED" &&
                                selectedIssue.status !== "CLOSED" && (
                                    <button
                                        type="button"
                                        className="incident-resolve-button"
                                        onClick={() =>
                                            handleStatusChange(
                                                "RESOLVED"
                                            )
                                        }
                                        disabled={actionLoading}
                                    >
                                        ✓ Resolve
                                    </button>
                                )}

                            {(selectedIssue.status === "RESOLVED" ||
                                selectedIssue.status === "CLOSED") && (
                                <div className="incident-completed-message">
                                    ✓ Incident completed
                                </div>
                            )}

                        </div>

                    </div>

                </div>
            )}

        </div>
    );
}

export default Dashboard;