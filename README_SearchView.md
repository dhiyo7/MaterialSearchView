# lib-název

Popisek.
 - funkce `funkce`

## Použití
```groovy
dependencies {
     compile 'cz.seznam:lib-nazev:x.x.x'
}
```
## Specifikace
  - min API 16
 
### Závislosti
 - "com.google.android.exoplayer:exoplayer-core"
 
## Inicializace, Funkce a volání knihovny, Popis
Služba ...

### Volání pomocí XML
```xml
    <cz.seznam.widget.customview
        android:id="@+id/customView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```
### Volání pomocí Java kódu
```java
Java kód
```





**SearchView.Version.MENU_ITEM and SearchView.Version.TOOLBAR:**
```java


```

**SearchView.Version.MENU_ITEM:**
```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case R.id.action_search:
            mSearchView.open();
            return true;
        default:
            return super.onOptionsItemSelected(item);
    }
}
```

**XML:**
```xml

```

**Styling SearchView:**
```xml
    <declare-styleable name="SearchView">
        <attr name="search_layout" format="reference" />
        <attr name="search_logo" format="enum">
            <enum name="google" value="1000" />
            <enum name="g" value="1001" />
            <enum name="hamburger" value="1002" />
            <enum name="arrow" value="1003" />
        </attr>
        <attr name="search_shape" format="enum">
            <enum name="classic" value="2000" />
            <enum name="rounded" value="2001" />
            <enum name="oval" value="2002" />
        </attr>
        <attr name="search_theme" format="enum">
            <enum name="color" value="3000" />
            <enum name="light" value="3001" />
            <enum name="dark" value="3002" />
        </attr>
        <attr name="search_version" format="enum">
            <enum name="toolbar" value="4000" />
            <enum name="menu_item" value="4001" />
        </attr>
        <attr name="search_version_margins" format="enum">
            <enum name="toolbar_small" value="5000" />
            <enum name="toolbar_big" value="5001" />
            <enum name="menu_item" value="5002" />
        </attr>
        <attr name="search_logo_icon" format="integer" />
        <attr name="search_logo_color" format="color" />
        <attr name="search_mic_icon" format="integer" />
        <attr name="search_mic_color" format="color" />
        <attr name="search_clear_icon" format="integer" />
        <attr name="search_clear_color" format="color" />
        <attr name="search_menu_icon" format="integer" />
        <attr name="search_menu_color" format="color" />
        <attr name="search_background_color" format="color" />
        <attr name="search_text_image" format="integer" />
        <attr name="search_text_color" format="color" />
        <attr name="search_text_size" format="dimension" />
        <attr name="search_text_style" format="enum">
            <enum name="normal" value="0" />
            <enum name="bold" value="1" />
            <enum name="italic" value="2" />
            <enum name="bold_italic" value="3" />
        </attr>
        <attr name="search_hint" format="string" />
        <attr name="search_hint_color" format="color" />
        <attr name="search_animation_duration" format="integer" />
        <attr name="search_shadow" format="boolean" />
        <attr name="search_shadow_color" format="color" />
        <attr name="search_elevation" format="dimension" />
    </declare-styleable>

    <declare-styleable name="SearchBar">
        <attr name="search_layout" />
        <attr name="search_logo" />
        <attr name="search_shape" />
        <attr name="search_theme" />
    </declare-styleable>
```

### Methods
| name | format | default | description
| ------ | ------ |  ------ |------ |
| setButtonsVisibility | boolean | true | Viditelnost všech tlačítek