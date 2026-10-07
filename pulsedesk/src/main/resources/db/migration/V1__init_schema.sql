CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL UNIQUE,
                       max_capacity_hours INTEGER NOT NULL DEFAULT 40,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_users (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           password_hash VARCHAR(255) NOT NULL,
                           role VARCHAR(30) NOT NULL DEFAULT 'USER',
                           team_id BIGINT,
                           workload_hours INTEGER NOT NULL DEFAULT 0,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_user_team
                               FOREIGN KEY (team_id)
                                   REFERENCES teams(id),

                           CONSTRAINT chk_user_role
                               CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE issues (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        description TEXT,
                        impact INTEGER NOT NULL,
                        urgency INTEGER NOT NULL,
                        affected_users INTEGER NOT NULL DEFAULT 0,
                        deadline DATE,
                        estimated_hours INTEGER NOT NULL DEFAULT 1,

                        priority_score INTEGER,
                        priority_level VARCHAR(20),

                        status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

                        created_by BIGINT NOT NULL,
                        assigned_to BIGINT,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_issue_creator
                            FOREIGN KEY (created_by)
                                REFERENCES app_users(id),

                        CONSTRAINT fk_issue_assignee
                            FOREIGN KEY (assigned_to)
                                REFERENCES app_users(id),

                        CONSTRAINT chk_issue_impact
                            CHECK (impact BETWEEN 1 AND 5),

                        CONSTRAINT chk_issue_urgency
                            CHECK (urgency BETWEEN 1 AND 5),

                        CONSTRAINT chk_issue_status
                            CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

CREATE TABLE issue_events (
                              id BIGSERIAL PRIMARY KEY,
                              issue_id BIGINT NOT NULL,
                              event_type VARCHAR(50) NOT NULL,
                              event_data TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_event_issue
                                  FOREIGN KEY (issue_id)
                                      REFERENCES issues(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_issues_status
    ON issues(status);

CREATE INDEX idx_issues_priority
    ON issues(priority_score);

CREATE INDEX idx_issues_assigned_to
    ON issues(assigned_to);

CREATE INDEX idx_issue_events_issue_id
    ON issue_events(issue_id);