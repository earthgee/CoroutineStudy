import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

public class CoroutineLesson3JavaCallCoroutine {

    public static void main(String[] args) {
        CoroutineLesson3_2Kt.notSuspend(new Continuation<Integer>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {
                if(o instanceof Integer) {
                    System.out.println(o);
                }
            }
        });
    }

}
