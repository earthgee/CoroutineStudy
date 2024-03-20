import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.DelayKt;
import org.jetbrains.annotations.NotNull;

public class ContinuationImpl implements Continuation<Object> {

    private int label = 0;
    private final Continuation<Unit> completion;

    public ContinuationImpl(Continuation<Unit> completion) {
        this.completion = completion;
    }

    @NotNull
    @Override
    public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
    }

    @Override
    public void resumeWith(@NotNull Object o) {
        try {
            Object result = o;
            switch (label) {
                case 0: {
                    MainKt.log(1);
                    result = ContinuationImplCaseKt.returnSuspended(this);
                    label ++;
                    if(isSuspended(result)) return;
                }
                case 1: {
                    MainKt.log(result);
                    MainKt.log(2);
                    result = DelayKt.delay(1000, this);
                    label ++;
                    if(isSuspended(result)) return;
                }
                case 2: {
                    MainKt.log(3);
                    result = ContinuationImplCaseKt.returnImmediately(this);
                    label ++;
                    if(isSuspended(result)) return;
                }
                case 3: {
                    MainKt.log(result);
                    MainKt.log(4);
                }
            }
            completion.resumeWith(Unit.INSTANCE);
        } catch (Exception e) {
            completion.resumeWith(e);
        }
    }

    private boolean isSuspended(Object result) {
        return result == IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }

    public static void main(String[] args) throws Throwable{
        JavaRunSuspend javaRunSuspend = new JavaRunSuspend();
        ContinuationImpl table = new ContinuationImpl(javaRunSuspend);
        table.resumeWith(Unit.INSTANCE);
        javaRunSuspend.await();
    }

}

