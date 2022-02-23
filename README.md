# iUi

- MVP
- ViewDataBinding
- EventBus
- OkHttp
- Retrofit
- CreateModel

---
### CreateModel Annotation
```
@CreateModel(baseUrl = "https://github.com/")
public interface IVersionApi {
    @GET("scliangml")
    Call<VersionBean> checkVersion(@Query("version") String version, @Query("need") boolean need);
}
```
用 @CreateModel(baseUrl = "https://github.com/") 注解IVersionApi接口，执行编译操作后，会自动生成如下两个Model相关的java文件(IVersionModel.java、VersionModel.java):
```
public interface IVersionModel extends IModel {
  void checkVersion(java.lang.String version, boolean need, ICallback<VersionBean> callback);
}
```
```
public class VersionModel implements IVersionModel {
  protected IVersionApi api;

  private List<Call> calls = new ArrayList<>();

  public VersionModel() {
    api = DataUtils.newApi(IVersionApi.class, "https://github.com/");
  }

  @Override
  public void cancelRequests() {
    synchronized (this) { for (Call call : calls) call.cancel(); calls.clear(); };
  }

  @Override
  public void checkVersion(java.lang.String version, boolean need,
      ICallback<VersionBean> callback) {
    Call<VersionBean> call = api.checkVersion(version, need);
    synchronized (this) { calls.add(call); };
    DataUtils.post(call, callback);
  }
}
```
接下来，只需要在DemoContract.java中引入接口和类即可：
```
public final class DemoContract {
    public interface IDemoView extends IView {
        void checkVersionSuccess(VersionBean data);
        void checkVersionFail(DataException error);
    }

    public interface IDemoPresenter extends IPresenter {
        void checkVersion();
    }

    public static final class DemoPresenter extends BasePresenter<IVersionModel, IDemoView> implements IDemoPresenter {
        public DemoPresenter(IDemoView view) {
            super(VersionModel.class, view);
        }

        @Override
        public void checkVersion() {
            getModel().checkVersion("", true, new ICallback<VersionBean>() {
                @Override
                public void onStart() {
                    getView().showLoading();
                }

                @Override
                public void onSuccess(VersionBean data) {
                    getView().checkVersionSuccess(data);
                }

                @Override
                public void onError(DataException error) {
                    getView().checkVersionFail(error);
                }

                @Override
                public void onCompleted() {
                    getView().dismissLoading();
                }
            });
        }
    }
}
```
