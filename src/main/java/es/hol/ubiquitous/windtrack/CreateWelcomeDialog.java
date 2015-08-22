package es.hol.ubiquitous.windtrack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by johna_000 on 28/5/2015.
 */
public class CreateWelcomeDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.about_us_message);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
