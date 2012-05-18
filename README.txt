Event-based execution monitor
=============================

Monitors periodic tasks by looking for evidence (in the form of events) indicating
recent changes.

For each task to be monitored, accepts configuration describing:
- the event type that will be generated 
- a filter constraining the events of the correct type that will be considered
- a maximum expected time between events for a healthy service


