package ch.ethz.dymand;

public class TimerExample {

    public TimerExample(){
                /*
        //Create timer task
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                //runEachHourly();

                startTimerfor55mins();

                //TODO: Remove vibrator test in final version
                Vibrator v = (Vibrator)  context.getSystemService(VIBRATOR_SERVICE);
                v.vibrate(500); // Vibrate for 500 milliseconds

                //TODO: Remove Trigger message to be displayed
                if(msg != null){
                    msg.triggerMsg("Start of new hour");
                }

                Log.d("Scheduler","New hour start task performed on " + new Date());
            }
        };
        timer = new Timer("Timer");
        timer.schedule(repeatedTask, 5000, DELAY_FOR_60_MINS);
        //timer.scheduleAtFixedRate(repeatedTask, nextMondayDate.getTime(), DELAY_FOR_60_MINS);

        */

                        /*
        TimerTask task = new TimerTask() {
            public void run() {

//                try {
//                    collectData();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }

                //TODO: Remove vibrator test in final version
                Vibrator v = (Vibrator)  context.getSystemService(VIBRATOR_SERVICE);
                v.vibrate(500); // Vibrate for 500 milliseconds


                //TODO: Remove trigger message to be displayed
                msg.triggerMsg("Last 5 mins in hour");
                Log.d("Scheduler", "Last 5 mins task performed on: " + new Date() + "n" +
                        "Thread's name: " + Thread.currentThread().getName());

            }
        };

        Timer timer = new Timer("Timer");
        timer.schedule(task, DELAY_FOR_55_MINS);

        */
    }
}
