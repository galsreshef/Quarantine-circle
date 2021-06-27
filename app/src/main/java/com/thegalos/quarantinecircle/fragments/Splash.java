package com.thegalos.quarantinecircle.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;

import com.thegalos.quarantinecircle.R;

public class Splash extends Fragment {

    public Splash() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MotionLayout motionLayout = view.findViewById(R.id.motionLayout);
        motionLayout.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int i, int i1) { }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int i, int i1, float v) { }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int i) {
                requireFragmentManager().beginTransaction().replace(R.id.flAppFragment, new Main(), "Main").commit();
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) { }
        });
    }
}

