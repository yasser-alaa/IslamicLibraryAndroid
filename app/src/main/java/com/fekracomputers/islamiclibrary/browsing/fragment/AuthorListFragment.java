package com.fekracomputers.islamiclibrary.browsing.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import com.fekracomputers.islamiclibrary.R;
import com.fekracomputers.islamiclibrary.browsing.activity.BrowsingActivity;
import com.fekracomputers.islamiclibrary.browsing.adapters.AuthorRecyclerViewAdapter;
import com.fekracomputers.islamiclibrary.browsing.interfaces.BrowsingActivityListingFragment;
import com.fekracomputers.islamiclibrary.databases.BooksInformationDBContract;
import com.fekracomputers.islamiclibrary.databases.BooksInformationDbHelper;
import com.fekracomputers.islamiclibrary.databases.SQL;
import com.fekracomputers.islamiclibrary.model.AuthorInfo;
import com.fekracomputers.islamiclibrary.model.BookCatalogElement;
import com.fekracomputers.islamiclibrary.search.model.FTS.Util;

import static com.fekracomputers.islamiclibrary.R.layout.fragment_author_list;

/**
 * A fragment representing a list of {@link AuthorInfo}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnAuthorItemClickListener}
 * interface.
 */
public class AuthorListFragment
        extends Fragment
        implements BrowsingActivityListingFragment,
        SortListDialogFragment.OnSortDialogListener {
    public static final int GRID_LAYOUT_MANAGER = 0;
    public static final int LINEAR_LAYOUT_MANAGER = 1;
    private static final String TAG = "AuthorListFragment";
    private static final String KEY_SAVED_STATE_LAYOUT_TYPRE = "AuthorListFragmentLayoutManager";
    private static final int SPAN_COUNT = 3;
    private static final String KEY_SHARED_PREF_AUTHOR_LAYOUT_TYPE = "AuthorListFragmentLayoutType";
    private static final String KEY_Author_LIST_SORT_INDEX_ONLY = "AuthorListFragmentSortIndex";
    private static final String[][] mOrderBy = {
            new String[]{
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_DEATH_HIJRI_YEAR,
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_NAME},
            new String[]{
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_NAME,
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_DEATH_HIJRI_YEAR
            },
            new String[]{
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.ORDER_BY_NUMBER_OF_BOOKS,
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_DEATH_HIJRI_YEAR,
                    BooksInformationDBContract.AuthorEntry.TABLE_NAME + SQL.DOTSEPARATOR + BooksInformationDBContract.AuthorEntry.COLUMN_NAME_NAME,
            },
    };
    protected int mCurrentLayoutManagerType;
    protected RecyclerView mRecyclerView;
    protected AuthorRecyclerViewAdapter mAuthorRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private OnAuthorItemClickListener mListener;
    private BooksInformationDbHelper booksInformationDbHelper;
    private int mCurrentSortIndex;
    private String mSearchQuery;
    private int mSavedScrollPositionBeforSearch;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AuthorListFragment() {
    }

    public static AuthorListFragment newInstance() {


        return new AuthorListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        booksInformationDbHelper = BooksInformationDbHelper.getInstance(this.getContext());
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mCurrentLayoutManagerType = sharedPref.getInt(KEY_SHARED_PREF_AUTHOR_LAYOUT_TYPE, LINEAR_LAYOUT_MANAGER);

        //Restore the last layout Type
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = savedInstanceState.getInt(KEY_SAVED_STATE_LAYOUT_TYPRE);
        }

        switch (mCurrentLayoutManagerType) {
            case LINEAR_LAYOUT_MANAGER:
            default:
                mLayoutManager = new LinearLayoutManager(getContext());
            case GRID_LAYOUT_MANAGER:
                mLayoutManager = new LinearLayoutManager(getContext());
        }

        mCurrentSortIndex = sharedPref.getInt(KEY_Author_LIST_SORT_INDEX_ONLY, 0);
        Cursor authorInfoCursor = getCursor(mListener.shouldDisplayDownloadedOnly());
        mAuthorRecyclerViewAdapter = new AuthorRecyclerViewAdapter(authorInfoCursor,
                BooksInformationDBContract.AuthorEntry.COLUMN_NAME_ID, mListener, getContext());
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(fragment_author_list, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setAdapter(mAuthorRecyclerViewAdapter);
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_author_list, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setQueryHint(getString(R.string.hint_search_authors_names));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if ((mSearchQuery == null || mSearchQuery.isEmpty()) && !(query == null || query.isEmpty())) {
                    //first click on search icon
                    mSearchQuery = query;
                    mSavedScrollPositionBeforSearch = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition();
                } else if (!(mSearchQuery == null || mSearchQuery.isEmpty()) && (query == null || query.isEmpty())) {
                    //clearing the search query
                    mSearchQuery = query;
                    Cursor bookListCursor = getCursor(mListener.shouldDisplayDownloadedOnly());
                    mAuthorRecyclerViewAdapter.changeCursor(bookListCursor);
                    (mRecyclerView.getLayoutManager()).scrollToPosition(mSavedScrollPositionBeforSearch);
                } else {
                    mSearchQuery = query;
                    Cursor bookListCursor = getCursor(mListener.shouldDisplayDownloadedOnly());
                    mAuthorRecyclerViewAdapter.changeCursor(bookListCursor);
                }
                return true;
            }
        });

    }


    private Cursor getCursor(boolean downloadedOnly) {
        if (mSearchQuery != null && !mSearchQuery.isEmpty()) {
            return getCursor(mSearchQuery, downloadedOnly);
        } else {
            return booksInformationDbHelper.getAuthorsFiltered(null, null, mOrderBy[mCurrentSortIndex], downloadedOnly);
        }
    }

    private Cursor getCursor(String newText, boolean downloadedOnly) {
        return booksInformationDbHelper.getAuthorsFiltered(
                BooksInformationDBContract.AuthorsNamesTextSearch.TABLE_NAME + "." + BooksInformationDBContract.AuthorsNamesTextSearch.COLUMN_NAME_NAME + " match ? ",
                new String[]{Util.getSearchPrefixQueryString(newText)},
                mOrderBy[mCurrentSortIndex],
                downloadedOnly);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_rearrange) {

            if (mCurrentLayoutManagerType == GRID_LAYOUT_MANAGER) {
                setRecyclerViewLayoutManager(LINEAR_LAYOUT_MANAGER);
                item.setIcon(R.drawable.ic_view_stream_black_24dp);
            } else {//if mCurrentLayoutManagerType == LayoutManagerType.LINEAR_LAYOUT_MANAGER
                setRecyclerViewLayoutManager(GRID_LAYOUT_MANAGER);
                item.setIcon(R.drawable.ic_view_module_black_24dp);
            }

            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            SortListDialogFragment sortListDialogFragment = SortListDialogFragment.newInstance(R.array.author_list_sorting, mCurrentSortIndex);
            //see this answer http://stackoverflow.com/a/37794319/3061221
            FragmentManager fm = getChildFragmentManager();
            sortListDialogFragment.show(fm, "fragment_sort");
            return true;

        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void sortMethodSelected(int which) {
        mCurrentSortIndex = which;
        boolean downloadedOnly = mListener.shouldDisplayDownloadedOnly();
        Cursor bookListCursor = getCursor(downloadedOnly);
        mAuthorRecyclerViewAdapter.changeCursor(bookListCursor);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_Author_LIST_SORT_INDEX_ONLY, which);
        editor.apply();

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        ((BrowsingActivity) getActivity()).unRegisterListener(this);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAuthorItemClickListener) {

            mListener = (OnAuthorItemClickListener) context;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCategoryItemClickListener");
        }
        if (context instanceof BrowsingActivity) {
            ((BrowsingActivity) context).registerListener(this);
        }
    }


    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(int layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        switch (layoutManagerType) {
            case GRID_LAYOUT_MANAGER:
                mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
                mCurrentLayoutManagerType = GRID_LAYOUT_MANAGER;
                break;
            case LINEAR_LAYOUT_MANAGER:
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
                mLayoutManager = linearLayoutManager;
                mCurrentLayoutManagerType = LINEAR_LAYOUT_MANAGER;
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), linearLayoutManager.getOrientation());
                mRecyclerView.addItemDecoration(dividerItemDecoration);
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LINEAR_LAYOUT_MANAGER;
        }
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_SHARED_PREF_AUTHOR_LAYOUT_TYPE, layoutManagerType);
        editor.apply();

        mAuthorRecyclerViewAdapter.setLayoutManagerType(layoutManagerType);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_SAVED_STATE_LAYOUT_TYPRE, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void actionModeDestroyed() {
        mAuthorRecyclerViewAdapter.notifyDataSetChanged();
//
//        mAuthorRecyclerViewAdapter.notifyItemRangeChanged(0,
//                mAuthorRecyclerViewAdapter.getItemCount(),
//                new RecyclerViewPartialUpdatePayload(RecyclerViewPartialUpdatePayload.UPDATE_VISABILITY,false));
//        mAuthorRecyclerViewAdapter.notifyItemRangeChanged(0,
//                mAuthorRecyclerViewAdapter.getItemCount(),
//                new RecyclerViewPartialUpdatePayload(RecyclerViewPartialUpdatePayload.UPDATE_CHECKED,false));
    }

    @Override
    public void actionModeStarted() {
        mAuthorRecyclerViewAdapter.notifyDataSetChanged();
//
//        mAuthorRecyclerViewAdapter.notifyItemRangeChanged(0,
//                mAuthorRecyclerViewAdapter.getItemCount(),
//                new RecyclerViewPartialUpdatePayload(RecyclerViewPartialUpdatePayload.UPDATE_VISABILITY,true));
    }

    @Override
    public void switchTodownloadedOnly(boolean checked) {
        mAuthorRecyclerViewAdapter.changeCursor(getCursor(checked));
    }

    @Override
    public void reAcquireCursors() {
        boolean downloadedOnly = mListener.shouldDisplayDownloadedOnly();
        mAuthorRecyclerViewAdapter.changeCursor(getCursor(downloadedOnly));
        mAuthorRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void closeCursors() {
        mAuthorRecyclerViewAdapter.getCursor().close();
    }

    @Override
    public void selecteItem(BookCatalogElement bookCatalogElement) {
        //TODO Go to the specified author
    }


    @Override
    public void BookDownloadStatusUpdate(int bookId, int downloadStatus) {
        reAcquireCursors();

//        mAuthorRecyclerViewAdapter.notifyItemRangeChanged(0,
//                mAuthorRecyclerViewAdapter.getItemCount(),
//                new RecyclerViewPartialUpdatePayload(RecyclerViewPartialUpdatePayload.UPDATE_DOWNLOAD_STATUS,bookId,downloadStatus));
    }

    @Override
    public void bookSelectionStatusUpdate() {

//        mAuthorRecyclerViewAdapter.notifyItemRangeChanged(((LinearLayoutManager) mLayoutManager).findFirstVisibleItemPosition(),
//                ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition(),
//                new RecyclerViewPartialUpdatePayload(RecyclerViewPartialUpdatePayload.UPDATE_CHECKED));

        mAuthorRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public int getType() {
        return BrowsingActivity.AUTHOR_LIST_FRAGMENT_TYPE;
    }

    public enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnAuthorItemClickListener {
        void OnAuthorItemItemClick(AuthorInfo authorInfo);

        boolean isInSelectionMode();

        void onAuthorSelected(int authorId, boolean checked);

        boolean isAuthorSelected(int authorId);

        boolean OnAuthorItemLongClicked(int authorId);

        boolean shouldDisplayDownloadedOnly();
    }

}
