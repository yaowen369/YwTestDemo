package ywdemo.example.yaoxiaowen;

import java.util.ArrayList;
import java.util.List;

public interface ModuleManager {

    public static List<IModuleInterface> getModule() {
        List<IModuleInterface> modules = new ArrayList<IModuleInterface>();

        modules.add(new FinanceModule());
        // ... 再添加其他的业务线module

        return modules;
    }
}
