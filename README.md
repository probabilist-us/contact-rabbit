# contact-rabbit
Contact-rabbit reads a waypoint file produced by fractalrabbit, consisting of simulated waypoint data for multiple mobile devices over some number of days. Then contact-rabbit randomly samples a set of "infectors", and uses stochastic proximity-based mechanisms, based on waypoint history of all mobileIDs, to create a set of "targets" who become infected. The algorithmic challenge is: given the targets, determine the infectors. 

The runnable jar file uses the syntax:
java -jar contact-rabbit.jar waypointfile

Here waypointfile is a CSV file whose rows are:
mobileID, timestamp (days), placeID
