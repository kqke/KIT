package com.example.kit.ui;

import androidx.fragment.app.DialogFragment;

import android.app.TimePickerDialog;
import android.widget.TimePicker;

public class TimePickerFragment extends DialogFragment implements
        TimePickerDialog.OnTimeSetListener {

    public TimePickerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

    }
}
