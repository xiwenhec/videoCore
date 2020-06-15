package com.gpufast.camera;

import com.gpufast.utils.State;
import com.gpufast.utils.StateMachine;

public class CameraStateMachine extends StateMachine {

    public static final int CMD_1 = 1;
    public static final int CMD_2 = 2;
    public static final int CMD_3 = 3;
    public static final int CMD_4 = 4;
    public static final int CMD_5 = 5;

    public static CameraStateMachine makeUp() {
        CameraStateMachine sm = new CameraStateMachine("hsm1");
        sm.start();
        return sm;
    }

    protected CameraStateMachine(String name) {
        super(name);
    }

    class State1 extends State {
        @Override
        public void enter() {
            super.enter();
        }
    }
}
