
package com.app.huawei;

import android.app.FragmentTransaction;

import com.app.huawei.logger.LoggerActivity;

public class BaseActivity1 extends LoggerActivity
{
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void addLogFragment() {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final LogFragment fragment = new LogFragment();
        transaction.replace(R.id.framelog, fragment);
        transaction.commit();
    }
}
