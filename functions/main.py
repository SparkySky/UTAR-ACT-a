# Corrected imports
from firebase_admin import initialize_app, firestore, messaging
import google.cloud.firestore
from firebase_functions import firestore_fn, options

initialize_app()
options.set_global_options(region=options.SupportedRegion.ASIA_SOUTHEAST1)


@firestore_fn.on_document_created(document="events/{eventId}/registrations/{applicantId}")
def send_notification_on_new_applicant(
    event: firestore_fn.Event[firestore_fn.Change],
) -> None:
    """
    [V3 Corrected] Triggered when a new user registers for an event.
    """
    print("--- FUNCTION STARTED: send_notification_on_new_applicant ---")

    # 1. Get data from the registration document
    applicant_data = event.data
    if applicant_data is None:
        print("Error: No data in the new registration document.")
        return

    print(f"Step 1: Registration data received: {applicant_data}")
    applicant_name = applicant_data.get("userName")
    event_id = event.params["eventId"]
    applicant_id = event.params["applicantId"]
    print(f"Step 2: Extracted Event ID '{event_id}' and Applicant ID '{applicant_id}'")

    if not applicant_name:
        print(f"Error: The 'userName' field is missing in registration document {applicant_id}.")
        return

    # 2. Get data from the parent event document
    db = firestore.client()
    try:
        event_ref = db.collection("events").document(event_id)
        event_doc = event_ref.get()

        if not event_doc.exists:
            print(f"Error: Parent event document '{event_id}' was not found.")
            return

        # --- THIS IS THE CORRECTED LOGIC ---
        # Get the event data immediately after checking if it exists
        print(f"Step 3: Parent event document '{event_id}' found successfully.")
        event_data = event_doc.to_dict()
        organizer_id = event_data.get("organizerId")
        event_name = event_data.get("eventName")
        # ------------------------------------

        if not organizer_id:
            print(f"Error: The 'organizerId' field is missing in event document '{event_id}'.")
            return
        if not event_name:
            print(f"Error: The 'eventName' field is missing in event document '{event_id}'.")
            return

        print(f"Step 4: Found Organizer ID '{organizer_id}' and Event Name '{event_name}'")

        # --- SEND PUSH NOTIFICATION ---
        user_doc = db.collection("users").document(organizer_id).get()
        if user_doc.exists:
            fcm_token = user_doc.to_dict().get("fcmToken")
            if fcm_token:
                push_message = messaging.Message(
                    notification=messaging.Notification(
                        title="New Applicant!",
                        body=f"{applicant_name} has registered for {event_name}",
                    ),
                    token=fcm_token,
                )
                messaging.send(push_message)
                print(f"Successfully sent PUSH notification to organizer {organizer_id}")

        # --- SEND IN-APP NOTIFICATION ---
        message = f"New applicant {applicant_name} has registered for <b>{event_name}</b>"
        notification = {
            "message": message,
            "eventId": event_id,
            "organizerId": organizer_id,
            "timestamp": firestore.SERVER_TIMESTAMP,
            "isRead": False,
        }
        db.collection("users").document(organizer_id).collection("notifications").add(notification)
        print(f"--- SUCCESS: In-app notification sent to organizer {organizer_id}. ---")

    except Exception as e:
        print(f"--- CRITICAL ERROR: An exception occurred: {e} ---")



# You can leave your test_trigger function here for now
@firestore_fn.on_document_created(document="test_triggers/{docId}")
def test_trigger(event: firestore_fn.Event[firestore_fn.Change]) -> None:
    """A simple function to test if Firestore triggers are working at all."""
    print("--- !!! TEST TRIGGER FIRED SUCCESSFULLY !!! ---")
    print(f"Triggered by document: {event.params['docId']}")
    
    db = firestore.client()
    db.collection("test_logs").add({
        "message": "The simple test trigger fired successfully!",
        "timestamp": firestore.SERVER_TIMESTAMP,
    })