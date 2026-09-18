package com.redbeard.poketrack;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CatalogRepository {
    private static final String[] SEASONS = {
            "Pokémon: Indigo League",
            "Pokémon - Oltre i cieli dell'avventura",
            "Always Pokémon - The Johto Journeys",
            "Pokémon Johto League Champions",
            "Pokémon Master Quest",
            "Pokémon Advanced",
            "Pokémon Advanced Challenge",
            "Pokémon Advanced Battle",
            "Pokémon Battle Frontier",
            "Pokémon Diamante e Perla",
            "Pokémon DP Battle Dimension",
            "Pokémon DP Lotte Galattiche",
            "Pokémon DP I Vincitori della Lega di Sinnoh",
            "Pokémon Nero e Bianco",
            "Pokémon Nero e Bianco - Destini Rivali",
            "Pokémon Nero e Bianco - Avventure a Unima e altrove",
            "Pokémon XY",
            "Pokémon XY - Esplorazioni a Kalos",
            "Pokémon XYZ",
            "Pokémon Sole e Luna",
            "Pokémon Sole e Luna - Ultravventure",
            "Pokémon Sole e Luna - Ultraleggende",
            "Esplorazioni Pokémon",
            "Esplorazioni Pokémon Master",
            "Esplorazioni Pokémon Super",
            "Orizzonti Pokémon",
            "Orizzonti Pokémon - Stagione 2: Alla ricerca di Laqua",
            "Orizzonti Pokémon - Stagione 3: Aria di speranza"
    };

    static List<CatalogGroup> seed() {
        List<CatalogGroup> out = new ArrayList<>();
        for (int i = 0; i < SEASONS.length; i++) {
            out.add(new CatalogGroup("season-" + (i + 1), SEASONS[i], "SERIE", i + 1));
        }
        out.add(new CatalogGroup("origins", "Pokémon Le origini", "MINISERIE", 16.5));
        out.add(new CatalogGroup("generations", "Pokémon Generazioni", "MINISERIE", 19.5));
        out.add(new CatalogGroup("twilight", "Ali del crepuscolo", "MINISERIE", 22.5));
        out.add(new CatalogGroup("poketoon", "POKÉTOON", "MINISERIE", 23.4));
        out.add(new CatalogGroup("evolutions", "Evoluzioni Pokémon", "MINISERIE", 24.5));
        out.add(new CatalogGroup("hisuian", "La neve di Hisui", "MINISERIE", 25.2));
        out.add(new CatalogGroup("arceus", "Pokémon: Le cronache di Arceus", "SPECIALE", 25.4));
        out.add(new CatalogGroup("pathpeak", "Pokémon: Verso la cima", "MINISERIE", 26.12));
        out.add(new CatalogGroup("paldeanwinds", "Pokémon: Venti di Paldea", "MINISERIE", 26.2));
        out.add(new CatalogGroup("concierge", "La concierge Pokémon", "SERIE", 26.3));

        Map<String, CatalogGroup> map = map(out);
        staticSideContent(map);
        insertMovies(map);
        Collections.sort(out, Comparator.comparingDouble(g -> g.order));
        return out;
    }

    static List<CatalogGroup> download() throws Exception {
        List<CatalogGroup> out = seed();
        Map<String, CatalogGroup> map = map(out);

        for (int s = 1; s <= 25; s++) {
            CatalogGroup group = map.get("season-" + s);
            if (group == null) continue;
            group.items.clear();
            String nn = String.format(Locale.US, "%02d", s);
            String url = s <= 6
                    ? "https://raw.githubusercontent.com/AustinGrech/Mikas-Pokesite/main/database/it/series-season" + nn + ".json"
                    : "https://raw.githubusercontent.com/seiya-dev/pokemon-tv/master/database/it/series-season" + nn + ".json";
            try {
                group.items.addAll(parsePokemonTv(http(url), s, "season-" + s, "EPISODIO"));
            } catch (Exception ignored) {
            }
        }

        loadPokemonTvOriginal(map, "origins", "original-origins.json");
        loadPokemonTvOriginal(map, "generations", "original-generations.json");
        loadPokemonTvOriginal(map, "twilight", "original-twilight-wings.json");
        loadPokemonTvOriginal(map, "poketoon", "original-poketoon.json");
        loadPokemonTvOriginal(map, "evolutions", "original-evolutions.json");
        loadPokemonTvOriginal(map, "hisuian", "original-hisuian-snow.json");
        loadPokemonTvOriginal(map, "pathpeak", "original-path-to-the-peak.json");

        try {
            loadHorizons(map);
        } catch (Exception ignored) {
        }

        staticSideContent(map);
        insertMovies(map);
        Collections.sort(out, Comparator.comparingDouble(g -> g.order));
        return out;
    }

    private static Map<String, CatalogGroup> map(List<CatalogGroup> groups) {
        Map<String, CatalogGroup> m = new HashMap<>();
        for (CatalogGroup g : groups) m.put(g.id, g);
        return m;
    }

    private static void loadPokemonTvOriginal(Map<String, CatalogGroup> map, String id, String file) {
        CatalogGroup g = map.get(id);
        if (g == null) return;
        try {
            List<ContentItem> items = parsePokemonTv(
                    http("https://raw.githubusercontent.com/seiya-dev/pokemon-tv/master/database/it/" + file),
                    0, id, "MINISERIE");
            if (!items.isEmpty()) {
                g.items.clear();
                for (ContentItem i : items) {
                    g.items.add(new ContentItem(i.id, "MINISERIE", i.title, 0, i.episode, g.title));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static List<ContentItem> parsePokemonTv(String json, int season, String prefix, String type) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray media = root.optJSONArray("media");
        List<ContentItem> out = new ArrayList<>();
        if (media == null) return out;
        for (int i = 0; i < media.length(); i++) {
            JSONObject e = media.optJSONObject(i);
            if (e == null) continue;
            int ep = parseInt(e.optString("episode"), i + 1);
            String title = e.optString("title").trim();
            if (title.isEmpty()) continue;
            out.add(new ContentItem(prefix + "-" + ep, type, title, season, ep, ""));
        }
        out.sort(Comparator.comparingInt(x -> x.episode));
        return out;
    }

    private static void loadHorizons(Map<String, CatalogGroup> map) throws Exception {
        for (int n = 1; n <= 3; n++) {
            CatalogGroup group = map.get("season-" + (25 + n));
            if (group == null) continue;
            String page = "S2S0" + n;
            String data = http("https://wiki.pokemoncentral.it/api.php?action=parse&page=" + page +
                    "&prop=wikitext&format=json&formatversion=2");
            String wt = new JSONObject(data).getJSONObject("parse").optString("wikitext");
            Pattern p = Pattern.compile("(?m)^\\|\\s*\\[\\[OP(\\d{3})[^\\]]*\\]\\]\\s*\\|\\|\\s*(?:\\[\\[)?([^\\]|\\n]+)");
            Matcher matcher = p.matcher(wt);
            List<ContentItem> items = new ArrayList<>();
            int local = 0;
            while (matcher.find()) {
                String title = matcher.group(2).replace("''", "").replaceAll("\\{\\{[^}]+}}", "").trim();
                if (title.isEmpty()) continue;
                local++;
                int op = parseInt(matcher.group(1), local);
                items.add(new ContentItem("horizons-op-" + op, "EPISODIO", title, 25 + n, local,
                        "OP" + String.format(Locale.US, "%03d", op)));
            }
            if (!items.isEmpty()) {
                group.items.clear();
                group.items.addAll(items);
            }
        }
    }

    private static void staticSideContent(Map<String, CatalogGroup> map) {
        putIfEmpty(map.get("pathpeak"), new String[]{"Il club", "Campionati Regionali", "Campionati Internazionali", "Campionati Mondiali"});
        putIfEmpty(map.get("paldeanwinds"), new String[]{"Espira", "Inspira", "Fai un bel respiro", "Respirare insieme"});
        putIfEmpty(map.get("hisuian"), new String[]{"Nel gelido blu", "Riflessi impetuosi nella neve", "Due sfumature"});
        putIfEmpty(map.get("concierge"), new String[]{
                "Sono Haru, la nuova concierge!", "Ti sei divertito, Psyduck?",
                "Anch'io vorrei tanto evolvermi…", "Benvenuto al Resort Pokémon!",
                "Un Pokémon in aiuto!", "È davvero la scelta migliore?",
                "Sono un esempio ora?", "Questa è casa mia"
        });
        CatalogGroup arceus = map.get("arceus");
        if (arceus != null && arceus.items.isEmpty()) {
            arceus.items.add(new ContentItem("special-arceus", "SPECIALE",
                    "Pokémon: Le cronache di Arceus", 0, 1, "Speciale"));
        }
    }

    private static void putIfEmpty(CatalogGroup g, String[] titles) {
        if (g == null || !g.items.isEmpty()) return;
        for (int i = 0; i < titles.length; i++) {
            g.items.add(new ContentItem(g.id + "-" + (i + 1), "MINISERIE", titles[i], 0, i + 1, g.title));
        }
    }

    private static void insertMovies(Map<String, CatalogGroup> map) {
        film(map, 1, 69, "Pokémon il film - Mewtwo contro Mew", "1998 · Film 1");
        film(map, 2, 24, "Pokémon 2 - La forza di uno", "1999 · Film 2");
        film(map, 3, 38, "Pokémon 3 - L'incantesimo degli Unown", "2000 · Film 3");
        special(map, 4, 19, "Mewtwo Returns", "2000 · Speciale TV");
        film(map, 4, 48, "Pokémon 4Ever", "2001 · Film 4");
        film(map, 5, 47, "Pokémon Heroes", "2002 · Film 5");
        film(map, 6, 35, "Pokémon: Jirachi Wish Maker", "2003 · Film 6");
        film(map, 7, 45, "Pokémon: Destiny Deoxys", "2004 · Film 7");
        film(map, 8, 43, "Pokémon: Lucario e il mistero di Mew", "2005 · Film 8");
        film(map, 9, 38, "Pokémon Ranger e il Tempio del Mare", "2006 · Film 9");
        film(map, 10, 39, "Pokémon: L'ascesa di Darkrai", "2007 · Film 10");
        film(map, 11, 34, "Pokémon: Giratina e il Guerriero dei Cieli", "2008 · Film 11");
        film(map, 12, 31, "Pokémon: Arceus e il Gioiello della Vita", "2009 · Film 12");
        film(map, 13, 21, "Pokémon: Il re delle illusioni Zoroark", "2010 · Film 13");
        film(map, 14, 39, "Il film Pokémon: Bianco - Victini e Zekrom", "2011 · Film 14A");
        film(map, 14, 39, "Il film Pokémon: Nero - Victini e Reshiram", "2011 · Film 14B");
        film(map, 15, 39, "Il film Pokémon - Kyurem e il solenne spadaccino", "2012 · Film 15");
        film(map, 16, 37, "Il film Pokémon - Genesect e il risveglio della leggenda", "2013 · Film 16");
        film(map, 17, 34, "Il film Pokémon - Diancie e il bozzolo della distruzione", "2014 · Film 17");
        film(map, 18, 33, "Il film Pokémon - Hoopa e lo scontro epocale", "2015 · Film 18");
        film(map, 19, 33, "Il film Pokémon - Volcanion e la meraviglia meccanica", "2016 · Film 19");
        film(map, 20, 33, "Il film Pokémon - Scelgo te!", "2017 · Film 20 · continuità alternativa");
        film(map, 21, 38, "Il film Pokémon - In ognuno di noi", "2018 · Film 21 · continuità alternativa");
        film(map, 22, 37, "Pokémon: Mewtwo colpisce ancora - L'evoluzione", "2019 · Film 22 · remake CGI");
        film(map, 24, 1, "Il film Pokémon - I segreti della giungla", "2020 · Film 23 · continuità alternativa");
    }

    private static void film(Map<String, CatalogGroup> map, int season, int after, String title, String note) {
        insert(map.get("season-" + season), after,
                new ContentItem("film-" + season + "-" + title.hashCode(), "FILM", title, season, 0, note));
    }

    private static void special(Map<String, CatalogGroup> map, int season, int after, String title, String note) {
        insert(map.get("season-" + season), after,
                new ContentItem("special-" + season + "-" + title.hashCode(), "SPECIALE", title, season, 0, note));
    }

    private static void insert(CatalogGroup g, int after, ContentItem item) {
        if (g == null) return;
        for (ContentItem x : g.items) {
            if (x.id.equals(item.id)) return;
        }
        int index = g.items.size();
        for (int i = 0; i < g.items.size(); i++) {
            ContentItem x = g.items.get(i);
            if ("EPISODIO".equals(x.type) && x.episode > after) {
                index = i;
                break;
            }
        }
        g.items.add(index, item);
    }

    private static String http(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(18000);
        c.setRequestProperty("User-Agent", "Poketrack/1.0 Android");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        try (InputStream in = c.getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return fallback;
        }
    }
}
