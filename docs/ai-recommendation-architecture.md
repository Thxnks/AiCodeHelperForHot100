# AI Recommendation Architecture

## Goal

The Hot100 module is not a pure sequential problem list. It builds a learner profile from progress records, recalls candidate problems with deterministic rules, then adds coach-style explanations and training focus.

## Data Flow

1. User marks problem progress through `POST /hot100/progress`.
2. Progress data is stored in `hot100_problem_progress`, including status, notes, wrong reason, knowledge point, AI feedback, and next action.
3. `Hot100ProgressService` computes:
   - weak tags from wrong answers and mastery rate
   - mastered tags from high mastery and low error count
   - recent wrong problems
   - candidate problems from the existing `recommendNext` recall logic
4. `GET /hot100/ai-recommendations` returns personalized recommendations with:
   - `coachSummary`
   - `reason`
   - `trainingFocus`
   - `trainingPlan`
5. `POST /hot100/wrong-book/analyze` delegates to `Hot100WrongAnalysisService`, which calls the LLM to analyze a wrong answer from problem context, user code, error description, and notes. The result is saved back into the progress table as wrong reason, knowledge point, AI feedback, and next action.
6. `AiController` injects the same learning profile into `system-prompt-role.txt`, so chat answers can adapt to the user's weak tags and recent wrong problems.

## Interview Explanation

The architecture is a hybrid recommender:

- Rule layer: recalls candidate problems by weak tags and unfinished problems. This keeps results stable and explainable.
- Profile layer: aggregates tag mastery, wrong answers, and recent mistakes into a user learning profile.
- Coach layer: turns profile signals into recommendation reasons and training focus.
- LLM layer: wrong-answer analysis calls the model to produce structured diagnosis; the chat system also consumes the user profile and current problem context, so AI tutoring is personalized instead of only answering the current question.

This design avoids letting the model directly pick from the whole problem set, which would be harder to control and harder to debug.

## AI Call Governance

Wrong-answer analysis is not implemented as `Controller -> model API -> response`.

The backend adds a governance layer:

1. `Hot100WrongAnalysisService` validates the request and builds an enriched prompt from problem metadata plus user evidence.
2. The model is required to return structured JSON.
3. The service parses and validates the JSON response.
4. If parsing fails, it calls a JSON repair prompt once.
5. If repair still fails or the model call throws, it returns a conservative fallback analysis instead of breaking the user flow.
6. Every call is recorded in `ai_call_log` with user id, business type, problem slug, request hash, latency, success flag, repaired flag, fallback flag, and error message.

This makes the LLM an internal reasoning engine, while the backend owns stability, observability, persistence, and business integration.
