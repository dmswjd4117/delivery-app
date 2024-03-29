package com.android.delivery.view.store;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.android.delivery.R;
import com.android.delivery.adapter.menu.GroupMenuAdapter;
import com.android.delivery.adapter.menu.GroupMenuItem;
import com.android.delivery.adapter.menu.MenuItem;
import com.android.delivery.api.MenuApi;
import com.android.delivery.api.StoreSearchApi;
import com.android.delivery.databinding.ActivityStoreBinding;
import com.android.delivery.model.ResponseDto;
import com.android.delivery.model.menu.GroupMenuDto;
import com.android.delivery.model.menu.MenuDto;
import com.android.delivery.model.store.StoreInfoDto;
import com.android.delivery.utils.RetrofitClient;
import com.android.delivery.view.cart.CartActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class StoreActivity extends AppCompatActivity {

    private String TAG = "STORE_ACTIVITY_TAG";
    private ActivityStoreBinding binding;
    private MenuApi menuApi;
    private StoreSearchApi storeSearchApi;
    private ExpandableListView groupMenuListView;
    private GroupMenuAdapter adapter;
    private Toolbar toolbar;
    private Long storeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        storeId = intent.getLongExtra("storeId",  -1L);

        menuApi = RetrofitClient.createService(MenuApi.class);
        storeSearchApi = RetrofitClient.createService(StoreSearchApi.class);

        groupMenuListView = binding.storeInfoGroupMenuList;
        adapter = new GroupMenuAdapter(this);

        toolbar = binding.toolbar;
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);


        initTab();
        initView();
        setMenuListView();
        getMenu(storeId);



        // cart btn
        binding.cartItemBtn.setOnClickListener(view -> {
            Intent cartIntent = new Intent(getApplicationContext(), CartActivity.class);
            startActivity(cartIntent);
        });

    }

    private void initTab(){
        TabHost tabHost = (TabHost) binding.storeInfoTabHost;
        tabHost.setup();

        TabHost.TabSpec menuTab = tabHost.newTabSpec("menuTab");
        menuTab.setContent(R.id.storeInfo_menuContent);
        menuTab.setIndicator("메뉴");

        TabHost.TabSpec infoTab = tabHost.newTabSpec("infoTab");
        infoTab.setContent(R.id.storeInfo_infoContent);
        infoTab.setIndicator("정보");

        TabHost.TabSpec reviewTab = tabHost.newTabSpec("reviewTab");
        reviewTab.setContent(R.id.storeInfo_reviewContent);
        reviewTab.setIndicator("리뷰");

        tabHost.addTab(menuTab);
        tabHost.addTab(infoTab);
        tabHost.addTab(reviewTab);
    }

    private void initView() {
        storeSearchApi.searchStoreById(storeId).enqueue(new Callback<ResponseDto>() {
                    @Override
                    public void onResponse(Call<ResponseDto> call, Response<ResponseDto> response) {
                        ResponseDto responseDto = response.body();
                        if(responseDto == null)return;
                        if(!responseDto.isSuccess()){
                            Log.e(TAG, responseDto.getError().getMessage());
                            return;
                        }

                        Log.i(TAG, responseDto.getResponse().toString());
                        Gson gson = new Gson();
                        Type storeInfoType = new TypeToken<StoreInfoDto>(){}.getType();
                        String jsonResult = gson.toJson(responseDto.getResponse());
                        StoreInfoDto storeInfo = gson.fromJson(jsonResult, storeInfoType);

                        binding.storeInfoTitle.setText(storeInfo.getName());
                        binding.storeInfoMinimumOrder.setText(storeInfo.getMinimumOrder()+"");
                        binding.storeInfoDeliveryTip.setText(storeInfo.getDeliveryTip()+"");
                    }

                    @Override
                    public void onFailure(Call<ResponseDto> call, Throwable t) {

                    }
        });
    }

    private void setMenuListView(){
        groupMenuListView.setAdapter(adapter);
        groupMenuListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return false;
            }
        });
        groupMenuListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Long menuId = adapter.getMenuId(groupPosition, childPosition);
                Log.i(TAG, menuId+" ");

                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);

                MenuItem menuItem = adapter.getChild(groupPosition, childPosition);
                intent.putExtra("menuId", menuItem.getId());
                intent.putExtra("name", menuItem.getName());
                intent.putExtra("photo", menuItem.getPhoto());
                intent.putExtra("description", menuItem.getDescription());
                intent.putExtra("price", menuItem.getPrice());
                startActivity(intent);

                return false;
            }
        });

    }


    private void getMenu(Long storeId){
        menuApi.getGroupMenuList(storeId).enqueue(new Callback<List<GroupMenuDto>>() {
            @Override
            public void onResponse(Call<List<GroupMenuDto>> call, Response<List<GroupMenuDto>> response) {
                List<GroupMenuDto> groupList = response.body();

                for(int groupIndex=0; groupIndex<groupList.size(); groupIndex++){
                    GroupMenuDto groupMenuDto = groupList.get(groupIndex);
                    int finalGroupIndex = groupIndex;


                    adapter.addGroupMenu(finalGroupIndex, groupMenuDto);


                    Long ID = groupMenuDto.getId();
                    menuApi.getMenuList(ID).enqueue(new Callback<List<MenuDto>>() {
                        @Override
                        public void onResponse(Call<List<MenuDto>> call, Response<List<MenuDto>> response) {
                            List<MenuDto> menuList = response.body();


                            for (int i = 0; i < menuList.size(); i++) {
                                MenuDto menu = menuList.get(i);
                                adapter.addMenu(finalGroupIndex, menu);
                            }
                            adapter.notifyDataSetChanged();


                            setListViewHeight();
                            groupMenuListView.expandGroup(finalGroupIndex);
                        }

                        @Override
                        public void onFailure(Call<List<MenuDto>> call, Throwable t) {

                        }
                    });

                }


            }

            @Override
            public void onFailure(Call<List<GroupMenuDto>> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });

    }


    private void setListViewHeight(){

        int result = adapter.getListLength(groupMenuListView);

        ViewGroup.LayoutParams params = groupMenuListView.getLayoutParams();
        params.height = result + (groupMenuListView.getDividerHeight() * (adapter.getGroupCount() - 1));

        Log.i(TAG, result+" "+params.height+" ");
        groupMenuListView.setLayoutParams(params);
        groupMenuListView.requestLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.basic_appbar_action, menu);
        return true;
    }
}