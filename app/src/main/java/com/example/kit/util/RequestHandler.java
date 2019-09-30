package com.example.kit.util;

import androidx.annotation.NonNull;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RequestHandler {

    public static void handleRequest(final Contact contact, final String display_name, final boolean accepted){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final FirebaseFirestore fs = FirebaseFirestore.getInstance();
                final String uid = FirebaseAuth.getInstance().getUid();
                final DocumentReference userRef = fs.collection(Constants.COLLECTION_USERS).document(uid);
                if (accepted){
                    userRef.collection(Constants.COLLECTION_CONTACTS).document(contact.getCid()).set(new Contact(display_name,
                            contact.getName(), contact.getAvatar(), contact.getCid(), contact.getStatus())).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
                            final DocumentReference contactRef =
                                    fs.collection(Constants.COLLECTION_USERS).document(contact.getCid());
                            contactRef.collection(Constants.COLLECTION_PENDING).document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        return;
                                    }
                                    final Contact ucontact = task.getResult().toObject(Contact.class);
                                    contactRef.collection(Constants.COLLECTION_CONTACTS).document(ucontact.getCid())
                                            .set(new Contact(ucontact.getName(), ucontact.getUsername(), ucontact.getAvatar(), ucontact.getCid(), ucontact.getStatus()))
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (!task.isSuccessful()) {
                                                return;
                                            }
                                            contactRef.collection(Constants.COLLECTION_PENDING).document(ucontact.getCid()).delete();
                                        }
                                    });
                                }
                            });
                        }

                    });
                }

                else {
                    userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
                    fs.collection(Constants.COLLECTION_USERS).document(contact.getCid()).collection(Constants.COLLECTION_PENDING).document(uid).delete();

                }
            }
        });

        thread.run();

    }



}
