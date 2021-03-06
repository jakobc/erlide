%% Author: jakob
%% Created: 30 aug 2010
%% Description: TODO: Add description to erlide_parsing_test
-module(erlide_parsing_tests).

%%
%% Include files
%%

-include_lib("eunit/include/eunit.hrl").
-include("erlide_scanner.hrl").

%%
%% Exported Functions
%%

-compile(export_all).

%%
%% API Functions
%%

%% records from erlide_noparse
%% TODO: should we move this to an interface somehow?

-record(model, {forms, comments}).

-record(function, {pos, name, arity, args, head, clauses, name_pos, comment, exported}).
%% -record(clause, {pos, name, args, head, name_pos}).
-record(attribute, {pos, name, args, extra}).
%% -record(other, {pos, name, tokens}).

parse_directive_test_() ->
    [?_assertEqual(#model{forms = [#attribute{pos = {{0,0,0},51},
                                              name = compile,
                                              args = u,
                                              extra = "[inline,{hipe,[{regalloc,linear_scan}]}]"}],
                          comments = []},
                   test_parse("-compile([inline,{hipe,[{regalloc,linear_scan}]}])."))].

parse_small_functions_test_() ->
    [?_assertEqual(#model{forms=[#function{pos = {{0,1,0},14},
                                           name = f, arity = 0,
                                           args = [], head = "", clauses = [],
                                           name_pos = {{0, 0}, 1},
                                           comment = undefined,
                                           exported = false}],
                          comments=[]},
                   test_parse("f() ->\n    a."))].

parsing_define_with_record_ref_test_() ->
    %% http://www.assembla.com/spaces/erlide/tickets/602-parser-can-t-handle-record-definitions-in-defines
    [?_assertEqual(#model{forms=[#attribute{pos={{0,0,0},18},
                                            name=define,
                                            args=xx,
                                            extra="xx, #x{}"}],
                          comments=[]},
                   test_parse("-define(xx, #x{})."))].

parsing_record_def_test_() ->
    Expected = #model{forms = [#attribute{pos = {{0, 0, 0}, 32},
                                          name = record,
                                          args = {a,[{b, {{0, 0, 12}, 1}},
                                                     {c, {{0, 0, 15}, 1}}]},
                                          extra = "a, {b, c :: integer()}"}],
                      comments = []},
    Value = test_parse("-record(a, {b, c :: integer()})."),
    [?_assertEqual(Expected, Value)].

parsing_record_ref_within_type_spec() ->
    S = "-record(a, {b, c :: #rec{}}).",
    Expected = {ok,[{"xxx",a,-4,[],false,20,4,false}]},
    Value = test_refs(S, [{record_ref, rec}]),
    [?_assertEqual(Expected, Value)].

%%
%% Local Functions
%%

test_parse(S) ->
    erlide_scanner_server:initialScan(testing, "", S, "/tmp", false),
    {ok, Res, unused} = erlide_noparse:reparse(testing),
    Res.

test_refs(S, SearchPattern) ->
    test_parse(S),
    erlide_search_server:find_refs([SearchPattern], [{testing, "xxx"}], "/tmp").

%% t() ->
%%     erlide_noparse:initial_parse(testing, ModuleFileName, StateDir, UpdateCaches, UpdateSearchServer),
